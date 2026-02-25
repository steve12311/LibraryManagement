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
    CaptchaVO getCaptcha();

    AuthenticationToken login(String username, String password);

    void logout();

    AuthenticationToken refreshToken(String refreshToken);
}
