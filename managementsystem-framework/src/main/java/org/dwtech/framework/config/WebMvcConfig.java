package org.dwtech.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * WebMvcConfig
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${oss.local.storage-path}")
    private String storagePath;

    /**
     * 用途：新增 resource handlers。
     * 
     * @param registry registry
     * 返回：无。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将本地存储路径映射为Web可访问的路径
        registry.addResourceHandler("/api/v1/files/uploads/**")
                .addResourceLocations("file:" + storagePath);
    }
}

