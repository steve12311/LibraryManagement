package org.dwtech.system.file.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * FileRefCountDeletePublisher
 *
 * @author steve12311
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileRefCountDeletePublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final FileRefCountDeleteProperties queueProperties;

    /**
     * 在事务提交后发布文件引用计数删除消息；若当前无事务则立即发布。
     *
     * @param message 队列消息
     */
    public void publishAfterCommit(FileRefCountDeleteMessage message) {
        Runnable publishAction = () -> {
            try {
                publishNow(message);
            } catch (RuntimeException exception) {
                log.warn(
                        "action=publish_file_refcount_delete result=degraded fileId={} retryCount={} exceptionType={}",
                        message.fileId(), message.retryCount(), exception.getClass().getSimpleName()
                );
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }
        publishAction.run();
    }

    /**
     * 立即向 Redis Stream 发布文件引用计数删除消息。
     *
     * @param message 队列消息
     */
    public void publishNow(FileRefCountDeleteMessage message) {
        Map<String, String> fields = message.toStreamFields();
        MapRecord<String, String, String> record = StreamRecords.mapBacked(fields)
                .withStreamKey(queueProperties.getStreamKey());
        redisTemplate.opsForStream().add(record);
    }
}
