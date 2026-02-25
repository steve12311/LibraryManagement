package org.dwtech.common.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.config.properties.MilvusProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * MilvusConfig
 *
 * @author steve12311
 * @since 2025-11-18
 */

@RequiredArgsConstructor
@Configuration
public class MilvusConfig {
    private final MilvusProperties milvusProperties;

    @Bean
    public MilvusClientV2 milvusServiceClient() {
        ConnectConfig config = ConnectConfig.builder()
                .username(milvusProperties.getUsername())
                .password(milvusProperties.getPassword())
                .uri(milvusProperties.getUri())
                .build();
        return new MilvusClientV2(config);
    }
}
