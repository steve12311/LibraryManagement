package org.dwtech.system.message.queue;

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
 * 消息队列发布器，负责将消息投递到 Redis Stream。
 * <p>
 * <b>事务安全保障</b>：
 * {@link #publishAfterCommit} 会在当前数据库事务提交后才推入 Redis Stream，
 * 确保 Consumer 拿到消息时，对应的 {@code sys_message_record} 已落盘可查。
 * 若当前无活跃事务，则立即发布。
 * <p>
 * Redis Stream Key 和 Consumer Group 由 {@link MessageStreamProperties} 配置。
 *
 * @author steve12311
 * @since 2026-05-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageStreamPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageStreamProperties queueProperties;

    /**
     * 事务提交后发布消息；若当前无事务则立即发布。
     * <p>
     * 通过 {@link TransactionSynchronizationManager#registerSynchronization} 注册
     * afterCommit 回调，保证消息记录已持久化后再入队，避免 Consumer 查不到记录。
     *
     * @param message 队列消息
     */
    public void publishAfterCommit(MessageStreamMessage message) {
        Runnable publishAction = () -> {
            try {
                publishNow(message);
            } catch (RuntimeException exception) {
                log.warn(
                        "action=publish_message_stream result=degraded recordId={} retryCount={} exceptionType={}",
                        message.recordId(), message.retryCount(), exception.getClass().getSimpleName()
                );
            }
        };
        // 检查是否有活跃事务同步，有则延迟到 afterCommit 执行
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
        // 无事务时立即发布
        publishAction.run();
    }

    /**
     * 立即向 Redis Stream 发布消息。
     *
     * @param message 队列消息
     */
    public void publishNow(MessageStreamMessage message) {
        Map<String, String> fields = message.toStreamFields();
        MapRecord<String, String, String> record = StreamRecords.mapBacked(fields)
                .withStreamKey(queueProperties.getStreamKey());
        redisTemplate.opsForStream().add(record);
    }
}
