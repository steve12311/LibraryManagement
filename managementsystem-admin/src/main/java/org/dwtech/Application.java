package org.dwtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * 图书管理系统启动入口
 * <p>
 * 基于 Spring Boot 3 + MyBatis-Plus + Spring Security 构建，
 * 启用定时任务（{@link EnableScheduling}）用于仪表盘指标汇聚，
 * 启用配置属性扫描（{@link ConfigurationPropertiesScan}）用于类型安全配置绑定。
 *
 * @author steve12311
 * @since 2025-10-30
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class Application {
    /**
     * 应用程序主入口，启动内嵌 Web 容器并自动装配所有模块。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
