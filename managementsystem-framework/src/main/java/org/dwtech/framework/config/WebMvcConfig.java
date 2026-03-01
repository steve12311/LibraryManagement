package org.dwtech.framework.config;

import cn.hutool.core.collection.CollectionUtil;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.config.properties.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * WebMvcConfig
 *
 * @author steve12311
 * @since 2026-02-22
 */

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final SecurityProperties securityProperties;

    /**
     * 配置跨域访问策略，启用凭证并仅允许白名单来源。
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        SecurityProperties.CorsConfig corsConfig = securityProperties.getCors();
        if (CollectionUtil.isEmpty(corsConfig.getAllowedOrigins())) {
            throw new IllegalStateException("security.cors.allowed-origins 不能为空");
        }
        if (corsConfig.getAllowedOrigins().stream().anyMatch(origin -> "*".equals(origin))) {
            throw new IllegalStateException("security.cors.allowed-origins 禁止使用通配符 *");
        }
        if (!Boolean.TRUE.equals(corsConfig.getAllowCredentials())) {
            throw new IllegalStateException("security.cors.allow-credentials 必须为 true");
        }

        registry.addMapping("/**")
                .allowedOrigins(corsConfig.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods(corsConfig.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(corsConfig.getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(Boolean.TRUE.equals(corsConfig.getAllowCredentials()))
                .maxAge(corsConfig.getMaxAge());
    }
}
