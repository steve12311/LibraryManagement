package org.dwtech.framework.auth.service;

import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.vo.CaptchaVO;

public interface AuthService {
    CaptchaVO getCaptcha();

    AuthenticationToken login(String username, String password);

    void logout();

    AuthenticationToken refreshToken(String refreshToken);
}
