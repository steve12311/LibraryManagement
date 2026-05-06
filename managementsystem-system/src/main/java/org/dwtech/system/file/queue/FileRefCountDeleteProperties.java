package org.dwtech.system.file.queue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * FileRefCountDeleteProperties
 *
 * @author steve12311
 * @since 2026-05-05
 */
@Data
@Validated
@ConfigurationProperties(prefix = "file.ref-count-delete-queue")
public class FileRefCountDeleteProperties {
    @NotBlank
    private String streamKey = "file:ref-count-delete:stream";
    @NotBlank
    private String consumerGroup = "file-ref-count-delete-group";
    @NotBlank
    private String consumerNamePrefix = "file-ref-count-delete-consumer";
    private boolean consumerEnabled = true;
    @Min(1)
    private int maxRetries = 3;
    @Min(1)
    private int pollTimeoutSeconds = 1;
}
