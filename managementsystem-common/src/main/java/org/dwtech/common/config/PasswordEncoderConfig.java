package org.dwtech.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
/**
 * PasswordEncoderConfig
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Configuration
public class PasswordEncoderConfig {

    /**
     * 用途：执行 password encoder 操作。
     * 
     * 密码编码器
     * 
     * 入参：无。
     * @return 返回结果
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}