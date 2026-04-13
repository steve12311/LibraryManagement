package org.dwtech.framework.ai.vectorstore;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * CatalogVectorQueueConsumer
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.catalog-vector-queue", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class CatalogVectorQueueConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final CatalogVectorQueueProperties queueProperties;
    private final BookService bookService;
    private final CatalogVectorStoreService catalogVectorStoreService;
    private final CatalogVectorQueuePublisher catalogVectorQueuePublisher;

    private volatile boolean running;
    private ExecutorService executorService;
    private String consumerName;

    /**
     * 用途：启动消费者线程。
     *
     * 返回：无。
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
     * 用途：停止消费者线程。
     *
     * 返回：无。
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
     * 用途：轮询消费 Redis Stream 消息。
     *
     * 返回：无。
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
     * 用途：消费一批消息。
     *
     * 返回：无。
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
     * 用途：处理单条消息记录。
     *
     * @param record Stream 记录
     * 返回：无。
     */
    void handleRecord(MapRecord<String, Object, Object> record) {
        CatalogVectorQueueMessage message = parseMessage(record.getValue());
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
     * 用途：按 ISBN 回查权威图书数据并同步或删除向量。
     *
     * @param message 队列消息
     * 返回：无。
     */
    void handleMessage(CatalogVectorQueueMessage message) {
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
     * 用途：处理消费失败后的重试或终止逻辑。
     *
     * @param recordId 记录 ID
     * @param message 队列消息
     * @param exception 异常
     * 返回：无。
     */
    private void handleProcessingFailure(RecordId recordId, CatalogVectorQueueMessage message, RuntimeException exception) {
        if (message.retryCount() < queueProperties.getMaxRetries()) {
            try {
                CatalogVectorQueueMessage retryMessage = message.nextRetry();
                catalogVectorQueuePublisher.publishNow(retryMessage);
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
     * 用途：确保 Redis Stream 消费组存在。
     *
     * 返回：无。
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
     * 用途：解析 Redis Stream 消息。
     *
     * @param fields Stream 字段
     * @return 返回结果
     */
    CatalogVectorQueueMessage parseMessage(Map<Object, Object> fields) {
        String isbn = asString(fields.get("isbn"));
        String triggerValue = asString(fields.get("trigger"));
        String retryCountValue = asString(fields.get("retryCount"));
        String occurredAt = asString(fields.get("occurredAt"));
        if (StrUtil.isBlank(isbn) || StrUtil.isBlank(triggerValue) || StrUtil.isBlank(retryCountValue)) {
            log.warn("action=parse_catalog_vector_queue result=skipped reason=missing_required_fields fields={}", fields);
            return null;
        }
        try {
            CatalogVectorQueueTrigger trigger = CatalogVectorQueueTrigger.valueOf(triggerValue);
            int retryCount = Integer.parseInt(retryCountValue);
            return new CatalogVectorQueueMessage(isbn, trigger, retryCount, occurredAt);
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
     * 用途：确认消费完成。
     *
     * @param recordId 记录 ID
     * 返回：无。
     */
    private void acknowledge(RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(
                queueProperties.getStreamKey(),
                queueProperties.getConsumerGroup(),
                recordId
        );
    }

    /**
     * 用途：判断是否为消费组已存在异常。
     *
     * @param exception 异常
     * @return 返回结果
     */
    private boolean isBusyGroupException(RuntimeException exception) {
        String message = exception.getMessage();
        return StrUtil.containsIgnoreCase(message, "BUSYGROUP");
    }

    /**
     * 用途：将对象转换为字符串。
     *
     * @param value 值
     * @return 返回结果
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 用途：在消费异常时短暂让出 CPU。
     *
     * 返回：无。
     */
    private void sleepQuietly() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
