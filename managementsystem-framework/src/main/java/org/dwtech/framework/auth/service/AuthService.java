package org.dwtech.framework.auth.service;

import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.auth.model.vo.CaptchaVO;
/**
 * 认证服务，提供验证码获取、登录、登出和令牌刷新功能。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface AuthService {
    /**
     * 获取验证码（图形验证码），用于登录前的安全验证。
     *
     * @return 验证码信息（图片 Base64 和验证码标识）
     */
    CaptchaVO getCaptcha();

    /**
     * 用户登录。验证用户名密码和验证码，成功时返回认证令牌。
     *
     * @param username 用户名
     * @param password 密码
     * @return 认证令牌（含 accessToken、refreshToken 和过期时间）
     */
    AuthenticationToken login(String username, String password);

    /**
     * 用户登出。清除当前用户的令牌和登录态。
     */
    void logout();

    /**
     * 刷新访问令牌。使用 refreshToken 换取新的 accessToken。
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证令牌
     */
    AuthenticationToken refreshToken(String refreshToken);
}
