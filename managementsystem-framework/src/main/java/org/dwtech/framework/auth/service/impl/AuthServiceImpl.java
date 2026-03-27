package org.dwtech.framework.auth.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.config.properties.CaptchaProperties;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.IResultCode;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.CaptchaTypeEnum;
import org.dwtech.auth.model.vo.CaptchaVO;
import org.dwtech.common.enmus.AuthEventTypeEnum;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.IPUtils;
import org.dwtech.common.utils.RefreshTokenCookieUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.common.utils.ServletUtils;
import org.dwtech.framework.auth.service.AuthService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.system.model.entity.AuthLogPO;
import org.dwtech.system.service.AuthLogService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

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
    private static final int MAX_FAILURE_SUMMARY_LENGTH = 200;

    private final TokenManager tokenManager;
    private final Font captchaFont;
    private final AuthenticationManager authenticationManager;
    private final CodeGenerator codeGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaProperties captchaProperties;
    private final SecurityProperties securityProperties;
    private final AuthLogService authLogService;

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
        String normalizedUsername = normalizeUsername(username);
        String clientIp = resolveClientIp();
        String sessionType = resolveSessionType();
        // 1. 创建用于密码认证的令牌（未认证）
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(normalizedUsername, password);
        Authentication authentication;
        try {
            // 2. 执行认证（认证中）
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                log.warn("认证失败, action=login, username={}, clientIp={}, sessionType={}, resultCode={}",
                        normalizedUsername, clientIp, sessionType, ResultCode.USER_PASSWORD_ERROR.getCode());
                recordAuthEvent(AuthEventTypeEnum.LOGIN.getValue(), null, normalizedUsername, 0,
                        ResultCode.USER_PASSWORD_ERROR.getCode(), "用户名或密码错误", clientIp, sessionType);
                throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "验证用户名密码失败");
            }
            if (e instanceof DisabledException) {
                log.warn("认证失败, action=login, username={}, clientIp={}, sessionType={}, resultCode={}",
                        normalizedUsername, clientIp, sessionType, ResultCode.USER_LOGIN_EXCEPTION.getCode());
                recordAuthEvent(AuthEventTypeEnum.LOGIN.getValue(), null, normalizedUsername, 0,
                        ResultCode.USER_LOGIN_EXCEPTION.getCode(), "账号已禁用", clientIp, sessionType);
                throw new BusinessException(ResultCode.USER_LOGIN_EXCEPTION, "账号已禁用");
            }
            log.error("认证异常, action=login, username={}, clientIp={}, sessionType={}, exceptionType={}",
                    normalizedUsername, clientIp, sessionType, e.getClass().getSimpleName());
            recordAuthEvent(AuthEventTypeEnum.LOGIN.getValue(), null, normalizedUsername, 0,
                    ResultCode.SYSTEM_ERROR.getCode(), "认证异常:" + e.getClass().getSimpleName(), clientIp, sessionType);
            throw new BusinessException("登录失败，请稍后再试");
        }
        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken authenticationTokenResponse =
                tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("认证成功, action=login, userId={}, username={}, clientIp={}, sessionType={}, result=success",
                resolveAuthenticatedUserId(authentication),
                resolveAuthenticatedUsername(authentication, normalizedUsername),
                clientIp,
                sessionType);
        recordAuthEvent(AuthEventTypeEnum.LOGIN.getValue(),
                resolveAuthenticatedUserId(authentication),
                resolveAuthenticatedUsername(authentication, normalizedUsername),
                1,
                ResultCode.SUCCESS.getCode(),
                null,
                clientIp,
                sessionType);
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
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        String clientIp = resolveClientIp();
        String sessionType = resolveSessionType();
        String accessToken = SecurityUtils.getTokenFromRequest();
        boolean hasAccessToken = StrUtil.isNotBlank(accessToken)
                && accessToken.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX);
        if (StrUtil.isNotBlank(accessToken) && accessToken.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            accessToken = accessToken.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
            tokenManager.invalidateToken(accessToken);
        }
        String refreshToken = null;
        if (RequestContextHolder.getRequestAttributes() != null) {
            refreshToken = RefreshTokenCookieUtils.getRefreshToken(ServletUtils.getRequest(), securityProperties);
        }
        boolean hasRefreshToken = StrUtil.isNotBlank(refreshToken);
        if (StrUtil.isNotBlank(refreshToken)) {
            tokenManager.invalidateToken(refreshToken);
        }
        SecurityContextHolder.clearContext();
        log.info("认证完成, action=logout, userId={}, username={}, clientIp={}, sessionType={}, result=success, hadAccessToken={}, hadRefreshToken={}",
                userId, username, clientIp, sessionType, hasAccessToken, hasRefreshToken);
        recordAuthEvent(AuthEventTypeEnum.LOGOUT.getValue(), userId, username, 1,
                ResultCode.SUCCESS.getCode(), null, clientIp, sessionType);
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
        String clientIp = resolveClientIp();
        String sessionType = resolveSessionType();
        try {
            AuthenticationToken token = tokenManager.refreshToken(refreshToken);
            log.info("认证成功, action=refresh_token, clientIp={}, sessionType={}, result=success",
                    clientIp, sessionType);
            recordAuthEvent(AuthEventTypeEnum.REFRESH_TOKEN.getValue(), null, null, 1,
                    ResultCode.SUCCESS.getCode(), null, clientIp, sessionType);
            return token;
        } catch (BusinessException e) {
            log.warn("认证失败, action=refresh_token, clientIp={}, sessionType={}, resultCode={}",
                    clientIp, sessionType, resolveResultCode(e));
            recordAuthEvent(AuthEventTypeEnum.REFRESH_TOKEN.getValue(), null, null, 0,
                    resolveResultCode(e), e.getMessage(), clientIp, sessionType);
            throw e;
        } catch (Exception e) {
            log.error("认证异常, action=refresh_token, clientIp={}, sessionType={}, exceptionType={}",
                    clientIp, sessionType, e.getClass().getSimpleName());
            recordAuthEvent(AuthEventTypeEnum.REFRESH_TOKEN.getValue(), null, null, 0,
                    ResultCode.SYSTEM_ERROR.getCode(), "认证异常:" + e.getClass().getSimpleName(), clientIp, sessionType);
            throw e;
        }
    }

    /**
     * 记录认证日志，失败不影响主流程。
     *
     * @param eventType 事件类型
     * @param userId 用户ID
     * @param username 用户名
     * @param success 是否成功
     * @param resultCode 结果码
     * @param failureSummary 失败摘要
     * @param clientIp 客户端IP
     * @param sessionType 会话模式
     */
    private void recordAuthEvent(String eventType,
                                 Long userId,
                                 String username,
                                 Integer success,
                                 String resultCode,
                                 String failureSummary,
                                 String clientIp,
                                 String sessionType) {
        AuthLogPO authLog = new AuthLogPO();
        authLog.setEventType(eventType);
        authLog.setUserId(userId);
        authLog.setUsername(username);
        authLog.setSuccess(success);
        authLog.setResultCode(resultCode);
        authLog.setFailureSummary(truncateFailureSummary(failureSummary));
        authLog.setClientIp(clientIp);
        authLog.setSessionType(sessionType);
        authLogService.saveQuietly(authLog);
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

    /**
     * 获取当前认证请求的客户端 IP。
     *
     * @return 客户端 IP，不可获取时返回 unknown
     */
    private String resolveClientIp() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return "unknown";
        }
        return StrUtil.blankToDefault(IPUtils.getIpAddr(ServletUtils.getRequest()), "unknown");
    }

    /**
     * 获取当前会话模式，便于日志统一识别认证方案。
     *
     * @return 会话模式名称
     */
    private String resolveSessionType() {
        if (securityProperties.getSession() == null) {
            return "unknown";
        }
        return StrUtil.blankToDefault(securityProperties.getSession().getType(), "unknown");
    }

    /**
     * 规范化用户名，避免日志与认证流程对空白值处理不一致。
     *
     * @param username 原始用户名
     * @return 规范化后的用户名
     */
    private String normalizeUsername(String username) {
        return StrUtil.blankToDefault(StrUtil.trim(username), "unknown");
    }

    /**
     * 从认证结果中提取用户 ID。
     *
     * @param authentication 认证结果
     * @return 用户 ID，未知时返回 null
     */
    private Long resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SysUserDetails userDetails)) {
            return null;
        }
        return userDetails.getUserId();
    }

    /**
     * 从认证结果中提取用户名。
     *
     * @param authentication 认证结果
     * @param fallbackUsername 回退用户名
     * @return 用户名
     */
    private String resolveAuthenticatedUsername(Authentication authentication, String fallbackUsername) {
        if (authentication == null) {
            return fallbackUsername;
        }
        if (authentication.getPrincipal() instanceof SysUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return StrUtil.blankToDefault(authentication.getName(), fallbackUsername);
    }

    /**
     * 从业务异常中提取业务码，避免直接记录异常消息。
     *
     * @param exception 业务异常
     * @return 业务码
     */
    private String resolveResultCode(BusinessException exception) {
        IResultCode resultCode = exception.getResultCode();
        return resultCode == null ? "UNKNOWN" : resultCode.getCode();
    }

    private String truncateFailureSummary(String failureSummary) {
        if (StrUtil.isBlank(failureSummary)) {
            return null;
        }
        return StrUtil.maxLength(failureSummary, MAX_FAILURE_SUMMARY_LENGTH);
    }
}
