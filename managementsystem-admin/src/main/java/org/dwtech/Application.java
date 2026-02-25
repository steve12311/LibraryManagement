package org.dwtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
/**
 * Application
 *
 * @author steve12311
 * @since 2025-10-30
 */

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {
    /**
     * 用途：执行 main 操作。
     * 
     * @param args args
     * 返回：无。
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
