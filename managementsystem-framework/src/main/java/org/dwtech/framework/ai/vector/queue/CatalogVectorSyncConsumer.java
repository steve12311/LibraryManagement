package org.dwtech.framework.ai.vector.queue;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.framework.ai.vector.store.CatalogVectorStoreService;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.service.BookService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CatalogVectorSyncConsumer
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.catalog-vector-queue", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class CatalogVectorSyncConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final CatalogVectorSyncProperties queueProperties;
    private final BookService bookService;
    private final CatalogVectorStoreService catalogVectorStoreService;
    private final CatalogVectorSyncPublisher catalogVectorSyncPublisher;

    private volatile boolean running;
    private ExecutorService executorService;
    private String consumerName;

    /**
     * 启动后台线程，持续消费 Redis Stream 中的向量同步消息。
     */
    @PostConstruct
    public void start() {
        ensureConsumerGroup();
        this.consumerName = queueProperties.getConsumerNamePrefix() + ":" + UUID.randomUUID();
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "catalog-vector-consumer");
            thread.setDaemon(true);
            return thread;
        });
        this.running = true;
        this.executorService.submit(this::consumeLoop);
    }

    /**
     * 优雅关闭消费者线程，等待正在处理的任务完成。
     */
    @PreDestroy
    public void stop() {
        this.running = false;
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            try {
                this.executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 消费者主循环：持续轮询 Redis Stream 获取并处理消息。
     */
    void consumeLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
            } catch (RuntimeException exception) {
                log.error(
                        "action=consume_catalog_vector_queue result=failed consumerName={} exceptionType={}",
                        consumerName,
                        exception.getClass().getSimpleName(),
                        exception
                );
                sleepQuietly();
            }
        }
    }

    /**
     * 从 Redis Stream 读取并处理一批待消费消息。
     */
    void pollOnce() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(queueProperties.getConsumerGroup(), consumerName),
                StreamReadOptions.empty()
                        .count(1)
                        .block(Duration.ofSeconds(queueProperties.getPollTimeoutSeconds())),
                StreamOffset.create(queueProperties.getStreamKey(), ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            handleRecord(record);
        }
    }

    /**
     * 解析并处理单条 Redis Stream 消息记录。
     *
     * @param record Redis Stream 消息记录
     */
    void handleRecord(MapRecord<String, Object, Object> record) {
        CatalogVectorSyncMessage message = parseMessage(record.getValue());
        if (message == null) {
            acknowledge(record.getId());
            return;
        }
        try {
            handleMessage(message);
            acknowledge(record.getId());
        } catch (RuntimeException exception) {
            handleProcessingFailure(record.getId(), message, exception);
        }
    }

    /**
     * 按 ISBN 回查权威图书数据，根据简介是否存在决定同步或删除向量。
     *
     * @param message 队列消息
     */
    void handleMessage(CatalogVectorSyncMessage message) {
        BookForm bookForm = bookService.getBookByIsbn(message.isbn());
        if (bookForm == null || StrUtil.isBlank(bookForm.getIntro())) {
            catalogVectorStoreService.deleteCatalogBook(message.isbn());
            return;
        }
        catalogVectorStoreService.syncCatalogBook(
                message.isbn(),
                bookForm.getName(),
                bookForm.getAuthor(),
                bookForm.getIntro()
        );
    }

    /**
     * 处理消费失败后的重试或终止逻辑：未达最大重试次数则重新投递，否则直接确认并记录日志。
     *
     * @param recordId  记录 ID
     * @param message   队列消息
     * @param exception 异常
     */
    private void handleProcessingFailure(RecordId recordId, CatalogVectorSyncMessage message, RuntimeException exception) {
        if (message.retryCount() < queueProperties.getMaxRetries()) {
            try {
                CatalogVectorSyncMessage retryMessage = message.nextRetry();
                catalogVectorSyncPublisher.publishNow(retryMessage);
                acknowledge(recordId);
                log.warn(
                        "action=consume_catalog_vector_queue result=retry_scheduled isbn={} trigger={} retryCount={} exceptionType={}",
                        message.isbn(),
                        message.trigger(),
                        retryMessage.retryCount(),
                        exception.getClass().getSimpleName()
                );
            } catch (RuntimeException retryException) {
                log.error(
                        "action=consume_catalog_vector_queue result=retry_publish_failed isbn={} trigger={} retryCount={} exceptionType={}",
                        message.isbn(),
                        message.trigger(),
                        message.retryCount(),
                        retryException.getClass().getSimpleName(),
                        retryException
                );
            }
            return;
        }
        acknowledge(recordId);
        log.error(
                "action=consume_catalog_vector_queue result=failed_exhausted isbn={} trigger={} retryCount={} exceptionType={}",
                message.isbn(),
                message.trigger(),
                message.retryCount(),
                exception.getClass().getSimpleName(),
                exception
        );
    }

    /**
     * 检查并创建 Redis Stream 消费组（不存在时自动创建）。
     */
    void ensureConsumerGroup() {
        RecordId bootstrapRecordId = redisTemplate.opsForStream().add(
                StreamRecords.mapBacked(Map.of("bootstrap", "catalog-vector"))
                        .withStreamKey(queueProperties.getStreamKey())
        );
        try {
            redisTemplate.opsForStream().createGroup(
                    queueProperties.getStreamKey(),
                    ReadOffset.latest(),
                    queueProperties.getConsumerGroup()
            );
        } catch (DataAccessException exception) {
            if (!isBusyGroupException(exception)) {
                throw exception;
            }
        } finally {
            if (bootstrapRecordId != null) {
                redisTemplate.opsForStream().delete(queueProperties.getStreamKey(), bootstrapRecordId);
            }
        }
    }

    /**
     * 将 Redis Stream 字段 Map 解析为 CatalogVectorSyncMessage 对象。
     *
     * @param fields Redis Stream 消息字段
     * @return 解析后的消息对象，字段不合法时返回 null
     */
    CatalogVectorSyncMessage parseMessage(Map<Object, Object> fields) {
        String isbn = asString(fields.get("isbn"));
        String triggerValue = asString(fields.get("trigger"));
        String retryCountValue = asString(fields.get("retryCount"));
        String occurredAt = asString(fields.get("occurredAt"));
        if (StrUtil.isBlank(isbn) || StrUtil.isBlank(triggerValue) || StrUtil.isBlank(retryCountValue)) {
            log.warn("action=parse_catalog_vector_queue result=skipped reason=missing_required_fields fields={}", fields);
            return null;
        }
        try {
            CatalogVectorSyncTrigger trigger = CatalogVectorSyncTrigger.valueOf(triggerValue);
            int retryCount = Integer.parseInt(retryCountValue);
            return new CatalogVectorSyncMessage(isbn, trigger, retryCount, occurredAt);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "action=parse_catalog_vector_queue result=skipped reason=invalid_field fields={} exceptionType={}",
                    fields,
                    exception.getClass().getSimpleName()
            );
            return null;
        }
    }

    /**
     * 向 Redis Stream 确认消息已成功消费。
     *
     * @param recordId 记录 ID
     */
    private void acknowledge(RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(
                queueProperties.getStreamKey(),
                queueProperties.getConsumerGroup(),
                recordId
        );
    }

    /**
     * 判断异常是否为消费组已存在（BUSYGROUP）异常。
     *
     * @param exception 异常
     * @return 是 BUSYGROUP 异常返回 true
     */
    private boolean isBusyGroupException(RuntimeException exception) {
        String message = exception.getMessage();
        return StrUtil.containsIgnoreCase(message, "BUSYGROUP");
    }

    /**
     * 将对象安全转换为字符串，null 时返回 null。
     *
     * @param value 待转换对象
     * @return 字符串或 null
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 消费异常时短暂休眠，避免空转导致的 CPU 飙升。
     */
    private void sleepQuietly() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
