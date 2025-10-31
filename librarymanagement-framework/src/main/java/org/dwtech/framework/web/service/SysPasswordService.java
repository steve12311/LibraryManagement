package org.dwtech.framework.web.service;

import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.CacheConstants;
import org.dwtech.common.core.entity.dto.SysUserDto;
import org.dwtech.common.core.redis.RedisCache;
import org.dwtech.common.exception.ServiceException;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.framework.security.AuthenticationContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SysPasswordService {
    private final RedisCache redisCache;

    @Value(value = "${user.password.maxRetryCount}")
    private int maxRetryCount;

    @Value(value = "${user.password.lockTime}")
    private int lockTime;

    public SysPasswordService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    private String getCacheKey(String username) {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    public void clearLoginRecordCache(String loginName) {
        if (redisCache.hasKey(getCacheKey(loginName))) {
            redisCache.deleteObject(getCacheKey(loginName));
        }
    }

    public void validate(SysUserDto user) {
        Authentication usernamePasswordAuthenticationToken = AuthenticationContextHolder.getContext();
        String username = usernamePasswordAuthenticationToken.getName();
        String password = usernamePasswordAuthenticationToken.getCredentials().toString();

        Integer currentTryCount = redisCache.getCacheObject(getCacheKey(username));

        if (currentTryCount == null) {
            currentTryCount = 0;
        }
        if (currentTryCount >= maxRetryCount) {
            log.info("登录用户：{}，失败次数到达上限", username);
            throw new ServiceException("登录重试次数到达上限");
        }

        if (!matches(user, password)) {
            currentTryCount = currentTryCount + 1;
            redisCache.setCacheObject(getCacheKey(username), currentTryCount, lockTime, TimeUnit.MINUTES);
            log.info("登录用户：{}，输入密码错误", username);
            throw new RuntimeException("密码错误");
        } else {
            clearLoginRecordCache(username);
        }
    }

    public boolean matches(SysUserDto user, String rawPassword) {
        return SecurityUtils.matchesPassword(rawPassword, user.getPassword());
    }
}
