package org.dwtech.framework.ai.vector.queue;

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
 * CatalogVectorSyncPublisher
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogVectorSyncPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final CatalogVectorSyncProperties queueProperties;

    /**
     * 用途：在事务提交后发布馆藏向量同步消息；若当前无事务则立即发布。
     *
     * @param message 队列消息
     * 返回：无。
     */
    public void publishAfterCommit(CatalogVectorSyncMessage message) {
        Runnable publishAction = () -> {
            try {
                publishNow(message);
            } catch (RuntimeException exception) {
                log.warn(
                        "action=publish_catalog_vector_queue result=degraded isbn={} trigger={} retryCount={} exceptionType={}",
                        message.isbn(),
                        message.trigger(),
                        message.retryCount(),
                        exception.getClass().getSimpleName()
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
     * 用途：立即发布馆藏向量同步消息。
     *
     * @param message 队列消息
     * 返回：无。
     */
    public void publishNow(CatalogVectorSyncMessage message) {
        Map<String, String> fields = message.toStreamFields();
        MapRecord<String, String, String> record = StreamRecords.mapBacked(fields)
                .withStreamKey(queueProperties.getStreamKey());
        redisTemplate.opsForStream().add(record);
    }
}
