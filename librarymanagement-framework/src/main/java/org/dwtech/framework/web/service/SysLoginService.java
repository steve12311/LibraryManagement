package org.dwtech.framework.web.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.CacheConstants;
import org.dwtech.common.core.entity.LoginUser;
import org.dwtech.common.core.redis.RedisCache;
import org.dwtech.common.exception.ServiceException;
import org.dwtech.common.utils.IpUtils;
import org.dwtech.common.utils.StringUtils;
import org.dwtech.framework.security.AuthenticationContextHolder;
import org.dwtech.system.service.SysUserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

@Slf4j
@Component
public class SysLoginService {
    private final RedisCache redisCache;
    private final TokenService tokenService;
    private final SysUserService sysUserService;
    @Resource
    private AuthenticationManager authenticationManager;

    public SysLoginService(RedisCache redisCache, TokenService tokenService, SysUserService sysUserService) {
        this.redisCache = redisCache;
        this.tokenService = tokenService;
        this.sysUserService = sysUserService;
    }

    public String login(String username, String password, String code, String uuid) {
        // validateCaptcha(code, uuid);
        // loginCheck(username, password);
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                log.error("验证用户名密码失败：{}", e.getMessage());
            } else {
                log.error("错误：{}", e.getMessage());
            }
            throw new ServiceException(e.getMessage());
        } finally {
            AuthenticationContextHolder.clearContext();
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        return tokenService.createToken(loginUser);
    }

    public void validateCaptcha(String code, String uuid) {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        if (StringUtils.isEmpty(captcha)) {
            throw new ServiceException("验证码已过期");
        }
        redisCache.deleteObject(verifyKey);
        if (!captcha.equalsIgnoreCase(code)) {
            throw new ServiceException("验证码不正确");
        }
    }

    public void loginCheck(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new ServiceException("用户名和密码不能为空");
        }
    }

    public void recordLoginInfo(Long userId){
        sysUserService.updateLoginInfo(userId, IpUtils.getIpAddr());
    }
}
