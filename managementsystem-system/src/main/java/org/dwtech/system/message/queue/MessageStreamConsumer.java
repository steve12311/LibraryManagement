package org.dwtech.system.message.queue;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.message.MessageChannelSender;
import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.mapper.MessageRecordMapper;
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
 * Redis Stream 消息消费者，异步消费待发送的消息并调用对应渠道发送器投递。
 * <p>
 * <b>消费流程</b>：
 * <ol>
 *   <li>启动时创建 Consumer Group（{@link #ensureConsumerGroup}）</li>
 *   <li>单线程轮询 Redis Stream（{@link #consumeLoop} → {@link #pollOnce}）</li>
 *   <li>解析消息 → 查 DB 消息记录 → 按 channel 匹配 {@link MessageChannelSender} → 调用 send()</li>
 *   <li>成功：更新状态为 SENT(1) + ACK；失败：重试或标记 FAILED(2)</li>
 * </ol>
 * <p>
 * <b>重试机制</b>：
 * 发送失败时，若重试次数未超过 {@code maxRetries}（默认 3 次），
 * 将消息重新发布到 Stream（retryCount+1），原消息立即 ACK。
 * 超过最大重试次数后，标记为 FAILED 并记录错误信息。
 * <p>
 * 可通过 {@code message.queue.consumer-enabled=false} 禁用消费者。
 *
 * @author steve12311
 * @since 2026-05-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "message.queue", name = "consumer-enabled",
        havingValue = "true", matchIfMissing = true)
public class MessageStreamConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageStreamProperties queueProperties;
    private final MessageRecordMapper messageRecordMapper;
    private final MessageStreamPublisher messageStreamPublisher;
    private final List<MessageChannelSender> channelSenders;

    /** 消费线程运行标志，stop() 时设为 false 以优雅退出 */
    private volatile boolean running;
    private ExecutorService executorService;
    /** 消费者实例名，格式：prefix:UUID，用于 Redis Stream Consumer 标识 */
    private String consumerName;

    /**
     * 启动消费线程。
     * 步骤：1) 确保 Consumer Group 存在 → 2) 生成唯一消费者名 → 3) 启动守护线程
     */
    @PostConstruct
    public void start() {
        ensureConsumerGroup();
        this.consumerName = queueProperties.getConsumerNamePrefix() + ":" + UUID.randomUUID();
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "message-stream-consumer");
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

    /** 消费主循环：持续轮询直到 running=false 或线程被中断。异常时 sleep 1s 后重试 */
    void consumeLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
            } catch (RuntimeException exception) {
                log.error(
                        "action=consume_message_stream result=failed consumerName={} exceptionType={}",
                        consumerName,
                        exception.getClass().getSimpleName(),
                        exception
                );
                sleepQuietly();
            }
        }
    }

    /** 单次轮询：从 Stream 读取最多 1 条消息，阻塞等待 pollTimeoutSeconds */
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
     * 处理单条 Stream 消息（核心方法）。
     * 步骤：解析消息 → 查 DB 记录 → 匹配渠道发送器 → 执行投递 → 更新状态 + ACK
     */
    void handleRecord(MapRecord<String, Object, Object> record) {
        MessageStreamMessage message = parseMessage(record.getValue());
        if (message == null) {
            acknowledge(record.getId());
            return;
        }
        try {
            MessageRecordPO messageRecord = messageRecordMapper.selectById(message.recordId());
            if (messageRecord == null) {
                log.warn("action=consume_message_stream result=skipped reason=record_not_found recordId={}",
                        message.recordId());
                acknowledge(record.getId());
                return;
            }
            MessageChannelSender sender = resolveSender(messageRecord.getChannel());
            if (sender == null) {
                log.error("action=consume_message_stream result=skipped reason=no_sender channel={}",
                        messageRecord.getChannel());
                messageRecordMapper.updateStatusAndError(messageRecord.getId(), 2,
                        "No sender for channel: " + messageRecord.getChannel(), messageRecord.getRetryCount());
                acknowledge(record.getId());
                return;
            }
            sender.send(messageRecord);
            messageRecordMapper.updateStatusAndError(messageRecord.getId(), 1, null, messageRecord.getRetryCount());
            acknowledge(record.getId());
            log.info("action=consume_message_stream result=success recordId={} channel={} retryCount={}",
                    message.recordId(), messageRecord.getChannel(), message.retryCount());
        } catch (RuntimeException exception) {
            handleProcessingFailure(record.getId(), message, exception);
        }
    }

    private MessageChannelSender resolveSender(String channel) {
        for (MessageChannelSender sender : channelSenders) {
            if (sender.channel().getValue().equalsIgnoreCase(channel)) {
                return sender;
            }
        }
        return null;
    }

    /**
     * 处理投递失败：未超过最大重试次数则重新入队（retryCount+1），否则标记为 FAILED(2)。
     * 重试时原消息立即 ACK，新消息以新 Record ID 写入 Stream。
     */
    private void handleProcessingFailure(RecordId recordId, MessageStreamMessage message,
                                          RuntimeException exception) {
        if (message.retryCount() < queueProperties.getMaxRetries()) {
            try {
                messageStreamPublisher.publishNow(message.nextRetry());
                acknowledge(recordId);
                log.warn(
                        "action=consume_message_stream result=retry_scheduled recordId={} retryCount={} exceptionType={}",
                        message.recordId(), message.retryCount(), exception.getClass().getSimpleName()
                );
            } catch (RuntimeException retryException) {
                log.error(
                        "action=consume_message_stream result=retry_publish_failed recordId={} retryCount={} exceptionType={}",
                        message.recordId(), message.retryCount(), retryException.getClass().getSimpleName(),
                        retryException
                );
            }
            return;
        }
        messageRecordMapper.updateStatusAndError(message.recordId(), 2,
                exception.getMessage(), message.retryCount());
        acknowledge(recordId);
        log.error(
                "action=consume_message_stream result=failed_exhausted recordId={} retryCount={} exceptionType={}",
                message.recordId(), message.retryCount(), exception.getClass().getSimpleName(),
                exception
        );
    }

    /**
     * 确保 Redis Stream 的 Consumer Group 存在。
     * Redis 要求 Stream 至少有一条消息才能创建 Group，因此先写 bootstrap 消息，
     * 创建 Group 后删除。若 Group 已存在（BUSYGROUP）则忽略。
     */
    void ensureConsumerGroup() {
        RecordId bootstrapRecordId = redisTemplate.opsForStream().add(
                StreamRecords.mapBacked(Map.of("bootstrap", "message-stream"))
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

    MessageStreamMessage parseMessage(Map<Object, Object> fields) {
        String recordIdValue = asString(fields.get("recordId"));
        String retryCountValue = asString(fields.get("retryCount"));
        String occurredAt = asString(fields.get("occurredAt"));
        if (StrUtil.isBlank(recordIdValue) || StrUtil.isBlank(retryCountValue)) {
            log.warn("action=parse_message_stream result=skipped reason=missing_required_fields fields={}", fields);
            return null;
        }
        try {
            long recordId = Long.parseLong(recordIdValue);
            int retryCount = Integer.parseInt(retryCountValue);
            return new MessageStreamMessage(recordId, retryCount, occurredAt);
        } catch (NumberFormatException exception) {
            log.warn(
                    "action=parse_message_stream result=skipped reason=invalid_field fields={} exceptionType={}",
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
