package org.dwtech.common.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dwtech.common.config.properties.SecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Objects;

/**
 * Refresh Token Cookie 工具类
 */
public final class RefreshTokenCookieUtils {

    private RefreshTokenCookieUtils() {
    }

    /**
     * 从请求 Cookie 中读取 refresh token。
     *
     * @param request 请求对象
     * @param securityProperties 安全配置
     * @return refresh token，不存在则返回 null
     */
    public static String getRefreshToken(HttpServletRequest request, SecurityProperties securityProperties) {
        if (request == null || ArrayUtil.isEmpty(request.getCookies())) {
            return null;
        }
        String cookieName = securityProperties.getRefreshTokenCookie().getName();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> StrUtil.equals(cookie.getName(), cookieName))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 写入 refresh token Cookie。
     *
     * @param response 响应对象
     * @param securityProperties 安全配置
     * @param refreshToken refresh token 值
     */
    public static void writeRefreshTokenCookie(HttpServletResponse response,
                                               SecurityProperties securityProperties,
                                               String refreshToken) {
        int maxAge = securityProperties.getSession().getRefreshTokenTimeToLive();
        ResponseCookie cookie = buildCookie(securityProperties, refreshToken, maxAge);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清理 refresh token Cookie。
     *
     * @param response 响应对象
     * @param securityProperties 安全配置
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response, SecurityProperties securityProperties) {
        ResponseCookie cookie = buildCookie(securityProperties, "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static ResponseCookie buildCookie(SecurityProperties securityProperties, String value, int maxAge) {
        SecurityProperties.RefreshTokenCookieConfig cookieConfig = securityProperties.getRefreshTokenCookie();
//        validateCookieConfig(cookieConfig);
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieConfig.getName(), value)
                .httpOnly(cookieConfig.getHttpOnly())
                .secure(cookieConfig.getSecure())
                .path(cookieConfig.getPath())
                .sameSite(cookieConfig.getSameSite())
                .maxAge(maxAge);
        if (StrUtil.isNotBlank(cookieConfig.getDomain())) {
            builder.domain(cookieConfig.getDomain());
        }
        return builder.build();
    }

    private static void validateCookieConfig(SecurityProperties.RefreshTokenCookieConfig cookieConfig) {
        if (!Boolean.TRUE.equals(cookieConfig.getHttpOnly())) {
            throw new IllegalStateException("refresh token Cookie 必须设置 HttpOnly=true");
        }
        if (!Boolean.TRUE.equals(cookieConfig.getSecure())) {
            throw new IllegalStateException("refresh token Cookie 必须设置 Secure=true");
        }
        if (!StrUtil.equalsIgnoreCase(cookieConfig.getSameSite(), "None")) {
            throw new IllegalStateException("跨站场景 refresh token Cookie 必须设置 SameSite=None");
        }
        if (!StrUtil.equals(cookieConfig.getPath(), "/api/v1/auth/refresh-token")) {
            throw new IllegalStateException("refresh token Cookie Path 必须为 /api/v1/auth/refresh-token");
        }
        if (Objects.isNull(cookieConfig.getName()) || Objects.isNull(cookieConfig.getPath())) {
            throw new IllegalStateException("refresh token Cookie 配置不能为空");
        }
    }
}
