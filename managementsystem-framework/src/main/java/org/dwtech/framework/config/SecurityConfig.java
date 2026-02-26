package org.dwtech.framework.config;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.ArrayUtil;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.framework.security.filter.CaptchaValidationFilter;
import org.dwtech.framework.security.filter.TokenAuthenticationFilter;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.common.token.TokenManager;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
/**
 * SecurityConfig
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
    private final TokenManager tokenManager;
    private final SysUserDetailService sysUserDetailService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private final CodeGenerator codeGenerator;

    /**
     * 用途：执行 filter chain 操作。
     * 
     * @param httpSecurity http security
     * @return 返回结果
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(requests -> {
                    String[] ignoreUrls = securityProperties.getIgnoreUrls();
                    if (ArrayUtil.isNotEmpty(ignoreUrls)) {
                        requests.requestMatchers(ignoreUrls).permitAll();
                    }
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll();
                    requests.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
                    requests.anyRequest().authenticated();
                })
                .sessionManagement(configurer ->
                        configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 无状态认证，不使用 Session
                )
                .csrf(AbstractHttpConfigurer::disable)      // 禁用 CSRF 防护，前后端分离无需此防护机制
                .formLogin(AbstractHttpConfigurer::disable) // 禁用默认的表单登录功能，前后端分离采用 Token 认证方式
                .httpBasic(AbstractHttpConfigurer::disable) // 禁用 HTTP Basic 认证，避免弹窗式登录
                // 验证码校验过滤器
                .addFilterBefore(new CaptchaValidationFilter(redisTemplate, codeGenerator), UsernamePasswordAuthenticationFilter.class)
                // 验证和解析过滤器
                .addFilterBefore(new TokenAuthenticationFilter(tokenManager), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 用途：执行 web security customizer 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> {
            String[] unsecuredUrls = securityProperties.getUnsecuredUrls();
            if (ArrayUtil.isNotEmpty(unsecuredUrls)) {
                web.ignoring().requestMatchers(unsecuredUrls);
            }
        };
    }

    /**
     * 用途：执行 dao authentication provider 操作。
     * 
     * 默认密码认证的 Provider
     * 
     * 入参：无。
     * @return 返回结果
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(sysUserDetailService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    /**
     * 用途：执行 authentication manager 操作。
     * 
     * 认证管理器
     * 
     * @param daoAuthenticationProvider dao authentication provider
     * @return 返回结果
     */
    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider
    ) {
        return new ProviderManager(
                daoAuthenticationProvider
        );
    }
}
