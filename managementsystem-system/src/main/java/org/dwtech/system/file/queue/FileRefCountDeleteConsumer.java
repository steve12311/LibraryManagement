package org.dwtech.system.file.queue;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.service.FileService;
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
 * FileRefCountDeleteConsumer
 *
 * @author steve12311
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.ref-count-delete-queue", name = "consumer-enabled",
        havingValue = "true", matchIfMissing = true)
public class FileRefCountDeleteConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final FileRefCountDeleteProperties queueProperties;
    private final FileService fileService;
    private final FileRefCountDeletePublisher fileRefCountDeletePublisher;

    private volatile boolean running;
    private ExecutorService executorService;
    private String consumerName;

    @PostConstruct
    public void start() {
        ensureConsumerGroup();
        this.consumerName = queueProperties.getConsumerNamePrefix() + ":" + UUID.randomUUID();
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "file-ref-count-delete-consumer");
            thread.setDaemon(true);
            return thread;
        });
        this.running = true;
        this.executorService.submit(this::consumeLoop);
    }

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

    void consumeLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
            } catch (RuntimeException exception) {
                log.error(
                        "action=consume_file_refcount_delete result=failed consumerName={} exceptionType={}",
                        consumerName,
                        exception.getClass().getSimpleName(),
                        exception
                );
                sleepQuietly();
            }
        }
    }

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

    void handleRecord(MapRecord<String, Object, Object> record) {
        FileRefCountDeleteMessage message = parseMessage(record.getValue());
        if (message == null) {
            acknowledge(record.getId());
            return;
        }
        try {
            fileService.deleteFileByRefCount(message.fileId());
            acknowledge(record.getId());
            log.info("action=consume_file_refcount_delete result=success fileId={} retryCount={}",
                    message.fileId(), message.retryCount());
        } catch (RuntimeException exception) {
            handleProcessingFailure(record.getId(), message, exception);
        }
    }

    private void handleProcessingFailure(RecordId recordId, FileRefCountDeleteMessage message,
                                          RuntimeException exception) {
        if (message.retryCount() < queueProperties.getMaxRetries()) {
            try {
                fileRefCountDeletePublisher.publishNow(message.nextRetry());
                acknowledge(recordId);
                log.warn(
                        "action=consume_file_refcount_delete result=retry_scheduled fileId={} retryCount={} exceptionType={}",
                        message.fileId(), message.retryCount(), exception.getClass().getSimpleName()
                );
            } catch (RuntimeException retryException) {
                log.error(
                        "action=consume_file_refcount_delete result=retry_publish_failed fileId={} retryCount={} exceptionType={}",
                        message.fileId(), message.retryCount(), retryException.getClass().getSimpleName(),
                        retryException
                );
            }
            return;
        }
        acknowledge(recordId);
        log.error(
                "action=consume_file_refcount_delete result=failed_exhausted fileId={} retryCount={} exceptionType={}",
                message.fileId(), message.retryCount(), exception.getClass().getSimpleName(),
                exception
        );
    }

    void ensureConsumerGroup() {
        RecordId bootstrapRecordId = redisTemplate.opsForStream().add(
                StreamRecords.mapBacked(Map.of("bootstrap", "file-ref-count-delete"))
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

    FileRefCountDeleteMessage parseMessage(Map<Object, Object> fields) {
        String fileIdValue = asString(fields.get("fileId"));
        String retryCountValue = asString(fields.get("retryCount"));
        String occurredAt = asString(fields.get("occurredAt"));
        if (StrUtil.isBlank(fileIdValue) || StrUtil.isBlank(retryCountValue)) {
            log.warn("action=parse_file_refcount_delete result=skipped reason=missing_required_fields fields={}", fields);
            return null;
        }
        try {
            long fileId = Long.parseLong(fileIdValue);
            int retryCount = Integer.parseInt(retryCountValue);
            return new FileRefCountDeleteMessage(fileId, retryCount, occurredAt);
        } catch (NumberFormatException exception) {
            log.warn(
                    "action=parse_file_refcount_delete result=skipped reason=invalid_field fields={} exceptionType={}",
                    fields,
                    exception.getClass().getSimpleName()
            );
            return null;
        }
    }

    private void acknowledge(RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(
                queueProperties.getStreamKey(),
                queueProperties.getConsumerGroup(),
                recordId
        );
    }

    private boolean isBusyGroupException(RuntimeException exception) {
        String message = exception.getMessage();
        return StrUtil.containsIgnoreCase(message, "BUSYGROUP");
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void sleepQuietly() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
