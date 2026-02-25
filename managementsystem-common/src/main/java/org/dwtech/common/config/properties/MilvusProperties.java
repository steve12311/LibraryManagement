package org.dwtech.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * MilvusProperties
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {
    private String uri;
    private String username;
    private String password;
    private int vectorDim;
    private String collectionName;
}
