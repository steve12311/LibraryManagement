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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
/**
 * Spring Security 核心配置 — 无状态 JWT 认证体系
 * <p>
 * 配置要点：
 * <ul>
 *   <li>禁用 Session、CSRF、表单登录、HTTP Basic，适配前后端分离架构</li>
 *   <li>过滤器链：验证码校验 → JWT 令牌解析 → 业务接口，均插入 {@code UsernamePasswordAuthenticationFilter} 之前</li>
 *   <li>免鉴权 URL 通过 {@code SecurityProperties.ignoreUrls} 配置，文件读取 GET 端点直接放行</li>
 *   <li>开启方法级权限注解（{@link EnableMethodSecurity}），配合 {@code @PreAuthorize} 使用</li>
 * </ul>
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
     * 构建核心安全过滤器链。
     * <p>
     * 执行顺序：验证码过滤器 → JWT 过滤器 → {@code UsernamePasswordAuthenticationFilter}（兜底认证）
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(requests -> {
                    String[] ignoreUrls = securityProperties.getIgnoreUrls();
                    if (ArrayUtil.isNotEmpty(ignoreUrls)) {
                        requests.requestMatchers(ignoreUrls).permitAll();
                    }
                    // 文件读取接口仅允许 GET，不强制认证
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/files/*").permitAll();
                    // 异步请求（如 SSE 流式对话）不拦截
                    requests.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
                    requests.anyRequest().authenticated();
                })
                .sessionManagement(configurer ->
                        configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 无状态，不创建 Session
                )
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)      // 前后端分离无 CSRF 风险
                .formLogin(AbstractHttpConfigurer::disable) // 禁用表单登录，采用 JWT 令牌认证
                .httpBasic(AbstractHttpConfigurer::disable) // 禁用 Basic 认证，避免弹窗
                .addFilterBefore(new CaptchaValidationFilter(redisTemplate, codeGenerator),
                        UsernamePasswordAuthenticationFilter.class) // ① 验证码校验
                .addFilterBefore(new TokenAuthenticationFilter(tokenManager),
                        UsernamePasswordAuthenticationFilter.class) // ② JWT 令牌解析与认证
                .build();
    }

    /**
     * 配置完全跳过 Security 过滤器链的 URL（如静态资源）。
     * 这些 URL 不会进入上述 {@code filterChain} 的任何过滤器。
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
     * DAO 认证提供者，负责从数据库加载用户信息并校验密码。
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(sysUserDetailService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    /**
     * 认证管理器，当前仅包含 DAO 认证提供者，后续可扩展（如短信登录、OAuth2）。
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }
}
