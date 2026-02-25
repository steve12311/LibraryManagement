package org.dwtech.framework.auth.service;

import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.auth.model.vo.CaptchaVO;

public interface AuthService {
    CaptchaVO getCaptcha();

    AuthenticationToken login(String username, String password);

    void logout();

    AuthenticationToken refreshToken(String refreshToken);
}
