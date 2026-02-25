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
import org.dwtech.auth.model.vo.CaptchaVO;
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
/**
 * AuthServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

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

    /**
     * 生成登录验证码并写入 Redis 缓存。
     *
     * <p>方法会按系统配置创建验证码对象，设置验证码生成器、透明度和字体，然后将验证码文本以随机键缓存到
     * Redis 中，过期时间由 {@code captchaProperties.expireSeconds} 控制，最后返回前端所需的键和 Base64 图片。</p>
     *
     * @return 验证码响应对象，包含验证码键和 Base64 图片内容
     */
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

    /**
     * 使用用户名和密码执行登录认证并签发令牌。
     *
     * <p>认证成功后会生成访问令牌/刷新令牌，并将认证结果写入 {@link SecurityContextHolder}，
     * 供后续鉴权链路和登录日志切面使用。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 认证令牌对象，包含访问令牌和刷新令牌
     * @throws BusinessException 用户名密码错误或认证过程出现其他业务异常
     */
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
     * 注销当前登录会话。
     *
     * <p>从请求中提取 Bearer Token 后加入黑名单，并清理 {@link SecurityContextHolder}，
     * 确保后续请求不会复用已失效的认证上下文。</p>
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
     * 使用刷新令牌换发新的访问令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 新的令牌对象，通常复用原刷新令牌并返回新的访问令牌
     * @throws BusinessException 刷新令牌无效或已过期时抛出
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        return tokenManager.refreshToken(refreshToken);
    }

    /**
     * 根据验证码配置创建对应类型的验证码实例。
     *
     * @return 验证码对象
     * @throws IllegalArgumentException 当配置的验证码类型不受支持时抛出
     */
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
