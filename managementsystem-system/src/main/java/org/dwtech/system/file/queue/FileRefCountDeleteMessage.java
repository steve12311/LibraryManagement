package org.dwtech.system.file.queue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FileRefCountDeleteMessage
 *
 * @author steve12311
 * @since 2026-05-05
 */
public record FileRefCountDeleteMessage(
        Long fileId,
        int retryCount,
        String occurredAt
) {

    public static FileRefCountDeleteMessage initial(Long fileId) {
        return new FileRefCountDeleteMessage(fileId, 0, Instant.now().toString());
    }

    public FileRefCountDeleteMessage nextRetry() {
        return new FileRefCountDeleteMessage(fileId, retryCount + 1, occurredAt);
    }

    public Map<String, String> toStreamFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("fileId", String.valueOf(fileId));
        fields.put("retryCount", String.valueOf(retryCount));
        fields.put("occurredAt", occurredAt);
        return fields;
    }
}
