package org.dwtech.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.Result;
import org.dwtech.auth.model.form.UserLoginForm;
import org.dwtech.auth.model.vo.CaptchaVO;
import org.dwtech.framework.auth.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/**
 * AuthController
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * 验证码生成
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> getKaptcha() {
        CaptchaVO captchaVO = authService.getCaptcha();
        return Result.success(captchaVO);
    }

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public Result<AuthenticationToken> login(@Validated @RequestBody UserLoginForm formData) {
        AuthenticationToken token = authService.login(formData.getUsername(), formData.getPassword());
        return Result.success(token);
    }

    @DeleteMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }

    @PostMapping("refresh-token")
    public Result<AuthenticationToken> refreshToken(
            @RequestParam("refreshToken") String refreshToken
    ) {
        AuthenticationToken authenticationToken = authService.refreshToken(refreshToken);
        return Result.success(authenticationToken);
    }
}
