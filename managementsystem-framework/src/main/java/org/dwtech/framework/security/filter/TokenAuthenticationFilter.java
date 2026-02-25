package org.dwtech.framework.security.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.utils.WebResponseHelper;
import org.dwtech.common.token.TokenManager;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token 认证校验过滤器
 *
 * @author steve12311
 * @since 2025-11-18
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Token 管理器
     */
    private final TokenManager tokenManager;

    /**
     * 在请求进入业务前完成访问令牌校验和认证上下文注入。
     *
     * <p>当请求头存在 Bearer Token 时，本方法会执行令牌有效性校验（包含验签与过期检查）。
     * 校验通过后将令牌解析为 {@link Authentication} 并写入 {@link SecurityContextHolder}；校验失败
     * 则直接写回未授权响应并中断过滤器链。</p>
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException 过滤器处理异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (StrUtil.isNotBlank(authorizationHeader)
                    && authorizationHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {

                // 剥离Bearer前缀获取原始令牌
                String rawToken = authorizationHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());

                // 执行令牌有效性检查（包含密码学验签和过期时间验证）
                boolean isValidToken = tokenManager.validateToken(rawToken);
                if (!isValidToken) {
                    WebResponseHelper.writeError(response, ResultCode.ACCESS_TOKEN_INVALID);
                    return;
                }

                // 将令牌解析为 Spring Security 上下文认证对象
                Authentication authentication = tokenManager.parseToken(rawToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // 安全上下文清除保障（防止上下文残留）
            SecurityContextHolder.clearContext();
            WebResponseHelper.writeError(response, ResultCode.ACCESS_TOKEN_INVALID);
            return;
        }

        // 继续后续过滤器链执行
        filterChain.doFilter(request, response);
    }
}
