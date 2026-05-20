package org.dwtech.system.message.queue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis Stream 消息体（不可变 record）。
 * <p>
 * 携带消息记录 ID、重试次数和发生时间，在 Publisher ↔ Consumer 之间传递。
 * 序列化为 Stream 的 Map 字段格式：{recordId, retryCount, occurredAt}。
 *
 * @param recordId   对应 sys_message_record 表的主键
 * @param retryCount 当前重试次数（初始为 0，每次重试 +1）
 * @param occurredAt 首次入队的 ISO-8601 时间戳（重试时不变）
 */
public record MessageStreamMessage(
        Long recordId,
        int retryCount,
        String occurredAt
) {

    /** 创建首次入队的消息（retryCount=0） */
    public static MessageStreamMessage initial(Long recordId) {
        return new MessageStreamMessage(recordId, 0, Instant.now().toString());
    }

    /** 创建重试消息（retryCount+1，保留原始 occurredAt） */
    public MessageStreamMessage nextRetry() {
        return new MessageStreamMessage(recordId, retryCount + 1, occurredAt);
    }

    /** 序列化为 Redis Stream 的 Map 字段 */
    public Map<String, String> toStreamFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("recordId", String.valueOf(recordId));
        fields.put("retryCount", String.valueOf(retryCount));
        fields.put("occurredAt", occurredAt);
        return fields;
    }
}
