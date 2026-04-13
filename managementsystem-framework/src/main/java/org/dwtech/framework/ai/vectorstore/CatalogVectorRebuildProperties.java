package org.dwtech.framework.ai.vectorstore;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CatalogVectorRebuildProperties
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.catalog-vector-rebuild")
public class CatalogVectorRebuildProperties {
    private boolean enabled = false;
    @Min(value = 1, message = "向量重建批大小必须大于 0")
    private int batchSize = 100;
}
