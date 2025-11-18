package org.dwtech.framework.auth.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.config.properties.CaptchaProperties;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.enmus.CaptchaTypeEnum;
import org.dwtech.common.core.entity.vo.CaptchaVO;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.framework.auth.service.AuthService;
import org.dwtech.common.token.TokenManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final TokenManager tokenManager;
    private final Font captchaFont;
    private final AuthenticationManager authenticationManager;
    private final CodeGenerator codeGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaProperties captchaProperties;

    @Override
    public CaptchaVO getCaptcha() {
        AbstractCaptcha captcha = getAbstractCaptcha();
        captcha.setGenerator(codeGenerator);
        captcha.setTextAlpha(captchaProperties.getTextAlpha());
        captcha.setFont(captchaFont);

        String captchaCode = captcha.getCode();
        String imageBase64Data = captcha.getImageBase64Data();

        // 验证码文本缓存至Redis，用于登录校验
        String captchaKey = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaKey),
                captchaCode,
                captchaProperties.getExpireSeconds(),
                TimeUnit.SECONDS
        );

        return CaptchaVO.builder()
                .captchaKey(captchaKey)
                .captchaBase64(imageBase64Data)
                .build();
    }

    @Override
    public AuthenticationToken login(String username, String password) {
        // 1. 创建用于密码认证的令牌（未认证）
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username.trim(), password);
        Authentication authentication;
        try {
            // 2. 执行认证（认证中）
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                log.error("验证用户名密码失败：{}", e.getMessage());
                throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "验证用户名密码失败");
            }
            log.error("错误：{}", e.getMessage());
            throw new BusinessException("错误：{}", e.getMessage());
        }
        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken authenticationTokenResponse =
                tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authenticationTokenResponse;
    }

    /**
     * 注销登录
     */
    @Override
    public void logout() {
        String token = SecurityUtils.getTokenFromRequest();
        if (StrUtil.isNotBlank(token) && token.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            token = token.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
            // 将JWT令牌加入黑名单
            tokenManager.invalidateToken(token);
            // 清除Security上下文
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 刷新token
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        return tokenManager.refreshToken(refreshToken);
    }

    private AbstractCaptcha getAbstractCaptcha() {
        String captchaType = captchaProperties.getType();
        int width = captchaProperties.getWidth();
        int height = captchaProperties.getHeight();
        int interfereCount = captchaProperties.getInterfereCount();
        int codeLength = captchaProperties.getCode().getLength();

        AbstractCaptcha captcha;
        if (CaptchaTypeEnum.CIRCLE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createCircleCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.GIF.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createGifCaptcha(width, height, codeLength);
        } else if (CaptchaTypeEnum.LINE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.SHEAR.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createShearCaptcha(width, height, codeLength, interfereCount);
        } else {
            throw new IllegalArgumentException("Invalid captcha type: " + captchaType);
        }
        return captcha;
    }
}
