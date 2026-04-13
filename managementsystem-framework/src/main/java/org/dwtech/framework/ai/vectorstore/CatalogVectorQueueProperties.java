package org.dwtech.framework.ai.vectorstore;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CatalogVectorQueueProperties
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.catalog-vector-queue")
public class CatalogVectorQueueProperties {
    @NotBlank(message = "馆藏向量队列 streamKey 不能为空")
    private String streamKey = "ai:catalog-vector:stream";
    @NotBlank(message = "馆藏向量队列 consumerGroup 不能为空")
    private String consumerGroup = "catalog-vector-group";
    @NotBlank(message = "馆藏向量队列 consumerNamePrefix 不能为空")
    private String consumerNamePrefix = "catalog-vector-consumer";
    private boolean consumerEnabled = true;
    @Min(value = 1, message = "馆藏向量队列重试次数必须大于 0")
    private int maxRetries = 3;
    @Min(value = 1, message = "馆藏向量队列轮询超时时间必须大于 0")
    private int pollTimeoutSeconds = 1;
}
