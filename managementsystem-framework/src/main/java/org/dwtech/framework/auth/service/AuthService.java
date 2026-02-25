package org.dwtech.framework.auth.service;

import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.auth.model.vo.CaptchaVO;
/**
 * AuthService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface AuthService {
    /**
     * 用途：获取 captcha 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    CaptchaVO getCaptcha();

    /**
     * 用途：执行 login 操作。
     * 
     * @param username username
     * @param password password
     * @return 返回结果
     */
    AuthenticationToken login(String username, String password);

    /**
     * 用途：执行 logout 操作。
     * 
     * 入参：无。
     * 返回：无。
     */
    void logout();

    /**
     * 用途：刷新 token。
     * 
     * @param refreshToken refresh token
     * @return 返回结果
     */
    AuthenticationToken refreshToken(String refreshToken);
}
