package org.dwtech.framework.ai.vectorstore;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CatalogVectorQueueMessage
 *
 * @author steve12311
 * @since 2026-04-13
 */
public record CatalogVectorQueueMessage(
        String isbn,
        CatalogVectorQueueTrigger trigger,
        int retryCount,
        String occurredAt
) {
    /**
     * 用途：构造首次投递的队列消息。
     *
     * @param isbn isbn
     * @param trigger 触发来源
     * @return 返回结果
     */
    public static CatalogVectorQueueMessage initial(String isbn, CatalogVectorQueueTrigger trigger) {
        return new CatalogVectorQueueMessage(isbn, trigger, 0, Instant.now().toString());
    }

    /**
     * 用途：构造下一次重试消息。
     *
     * @return 返回结果
     */
    public CatalogVectorQueueMessage nextRetry() {
        return new CatalogVectorQueueMessage(isbn, trigger, retryCount + 1, occurredAt);
    }

    /**
     * 用途：转换为 Redis Stream 字段。
     *
     * @return 返回结果
     */
    public Map<String, String> toStreamFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("isbn", isbn);
        fields.put("trigger", trigger.name());
        fields.put("retryCount", String.valueOf(retryCount));
        fields.put("occurredAt", occurredAt);
        return fields;
    }
}
