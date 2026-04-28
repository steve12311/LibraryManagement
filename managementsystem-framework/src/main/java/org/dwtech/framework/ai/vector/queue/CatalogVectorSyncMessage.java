package org.dwtech.framework.ai.vector.queue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CatalogVectorSyncMessage
 *
 * @author steve12311
 * @since 2026-04-13
 */
public record CatalogVectorSyncMessage(
        String isbn,
        CatalogVectorSyncTrigger trigger,
        int retryCount,
        String occurredAt
) {
    /**
     * 创建首次投递的向量同步消息（重试次数为 0）。
     *
     * @param isbn    图书 ISBN
     * @param trigger 触发来源
     * @return 初始消息实例
     */
    public static CatalogVectorSyncMessage initial(String isbn, CatalogVectorSyncTrigger trigger) {
        return new CatalogVectorSyncMessage(isbn, trigger, 0, Instant.now().toString());
    }

    /**
     * 创建重试次数加 1 的下一次重试消息。
     *
     * @return 重试消息实例
     */
    public CatalogVectorSyncMessage nextRetry() {
        return new CatalogVectorSyncMessage(isbn, trigger, retryCount + 1, occurredAt);
    }

    /**
     * 将消息转换为 Redis Stream 的字段 Map。
     *
     * @return 包含消息字段的 Map
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
