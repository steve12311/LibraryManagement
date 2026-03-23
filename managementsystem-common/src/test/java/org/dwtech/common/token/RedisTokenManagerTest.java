package org.dwtech.common.token;

import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.core.entity.OnlineUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisTokenManager redisTokenManager;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisTokenManager = new RedisTokenManager(buildSecurityProperties(), redisTemplate);
    }

    @Test
    void shouldInvalidateSessionWhenRefreshTokenProvided() {
        OnlineUser onlineUser = new OnlineUser(1001L, "alice", 2001L, 3, Set.of("ROLE_ADMIN"));
        when(valueOperations.get(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "refresh-token"))).thenReturn(null);
        when(valueOperations.get(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-token"))).thenReturn(onlineUser);
        when(valueOperations.get(RedisConstants.Auth.USER_ACCESS_TOKEN.replace("{}", "1001"))).thenReturn("access-token");
        when(valueOperations.get(RedisConstants.Auth.USER_REFRESH_TOKEN.replace("{}", "1001"))).thenReturn("refresh-token");

        redisTokenManager.invalidateToken("refresh-token");

        verify(redisTemplate).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-token"));
        verify(redisTemplate).delete(RedisConstants.Auth.USER_ACCESS_TOKEN.replace("{}", "1001"));
        verify(redisTemplate).delete(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-token"));
        verify(redisTemplate).delete(RedisConstants.Auth.USER_REFRESH_TOKEN.replace("{}", "1001"));
    }

    private SecurityProperties buildSecurityProperties() {
        SecurityProperties securityProperties = new SecurityProperties();
        SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
        sessionConfig.setType("redis-token");
        sessionConfig.setAccessTokenTimeToLive(1800);
        sessionConfig.setRefreshTokenTimeToLive(7200);
        SecurityProperties.RedisTokenConfig redisTokenConfig = new SecurityProperties.RedisTokenConfig();
        redisTokenConfig.setAllowMultiLogin(true);
        sessionConfig.setRedisToken(redisTokenConfig);
        securityProperties.setSession(sessionConfig);
        securityProperties.setIgnoreUrls(new String[]{"/api/v1/auth/**"});
        securityProperties.setUnsecuredUrls(new String[]{"/doc.html"});
        return securityProperties;
    }
}
