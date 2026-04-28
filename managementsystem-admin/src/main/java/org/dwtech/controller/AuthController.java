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
 * 认证控制器 — 登录/注销/刷新令牌/验证码
 * <p>
 * 对接 {@link AuthService} 完成认证流程，并通过 httpOnly Cookie 传递刷新令牌。
 * 登录和刷新令牌接口将刷新令牌写入 Cookie，注销时清除 Cookie。
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

    /** 获取图形验证码（含验证码 key 和 Base64 图片） */
    @GetMapping("/captcha")
    public Result<CaptchaVO> getKaptcha() {
        CaptchaVO captchaVO = authService.getCaptcha();
        return Result.success(captchaVO);
    }

    /**
     * 账号密码登录
     * <p>
     * 流程：验证码已在 {@code CaptchaValidationFilter} 中校验 →
     * {@code AuthService.login()} 执行密码认证并签发 JWT →
     * 将刷新令牌写入 httpOnly Cookie（防 XSS）。
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<AuthenticationToken> login(@Validated UserLoginForm formData, HttpServletResponse response) {
        AuthenticationToken token = authService.login(formData.getUsername(), formData.getPassword());
        RefreshTokenCookieUtils.writeRefreshTokenCookie(response, securityProperties, token.getRefreshToken());
        return Result.success(token);
    }

    /**
     * 注销登录
     * <p>
     * 流程：将访问令牌和 Cookie 中的刷新令牌加入 Redis 黑名单 →
     * 清除 Security 上下文 → 删除前端 Cookie。
     */
    @DeleteMapping("/logout")
    public Result<?> logout(HttpServletResponse response) {
        authService.logout();
        RefreshTokenCookieUtils.clearRefreshTokenCookie(response, securityProperties);
        return Result.success();
    }

    /**
     * 刷新令牌
     * <p>
     * 流程：从 Cookie 提取刷新令牌 → 校验有效性 → 签发新访问令牌 + 新刷新令牌（旧刷新令牌立即失效）→
     * 将新刷新令牌写入 Cookie。
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
