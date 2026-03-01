package org.dwtech.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.RefreshTokenCookieUtils;
import org.dwtech.auth.model.form.UserLoginForm;
import org.dwtech.auth.model.vo.CaptchaVO;
import org.dwtech.framework.auth.service.AuthService;
import org.springframework.http.MediaType;
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
    private final SecurityProperties securityProperties;

    /**
     * 用途：获取 kaptcha 信息。
     * 
     * 验证码生成
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> getKaptcha() {
        CaptchaVO captchaVO = authService.getCaptcha();
        return Result.success(captchaVO);
    }

    /**
     * 用途：执行 login 操作。
     * 
     * 账号密码登录（application/x-www-form-urlencoded）
     * 
     * @param formData form data
     * @return 返回结果
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<AuthenticationToken> login(@Validated UserLoginForm formData, HttpServletResponse response) {
        AuthenticationToken token = authService.login(formData.getUsername(), formData.getPassword());
        RefreshTokenCookieUtils.writeRefreshTokenCookie(response, securityProperties, token.getRefreshToken());
        return Result.success(token);
    }

    /**
     * 用途：执行 logout 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @DeleteMapping("/logout")
    public Result<?> logout(HttpServletResponse response) {
        authService.logout();
        RefreshTokenCookieUtils.clearRefreshTokenCookie(response, securityProperties);
        return Result.success();
    }

    /**
     * 用途：刷新 token。
     * 
     * @param request 请求数据
     * @param response 返回数据
     * @return 返回结果
     */
    @PostMapping("refresh-token")
    public Result<AuthenticationToken> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = RefreshTokenCookieUtils.getRefreshToken(request, securityProperties);
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        AuthenticationToken authenticationToken = authService.refreshToken(refreshToken);
        RefreshTokenCookieUtils.writeRefreshTokenCookie(response, securityProperties, authenticationToken.getRefreshToken());
        return Result.success(authenticationToken);
    }
}
