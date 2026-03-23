package org.dwtech.common.token;

import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.OnlineUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    private RedisTokenManager redisTokenManager;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(valueOperations.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        redisTokenManager = new RedisTokenManager(buildSecurityProperties(), redisTemplate);
    }

    @Test
    void shouldRefreshOnlyCurrentSessionWhenMultipleSessionsExist() {
        OnlineUser onlineUser = new OnlineUser(1001L, "alice", 2001L, 3, Set.of("ROLE_ADMIN"), System.currentTimeMillis());
        when(valueOperations.get(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-1"))).thenReturn(onlineUser);
        when(valueOperations.get(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"))).thenReturn("access-1");
        when(redisTemplate.getExpire(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-1"), TimeUnit.SECONDS)).thenReturn(7200L);

        AuthenticationToken token = redisTokenManager.refreshToken("refresh-1");

        assertThat(token.getRefreshToken()).isEqualTo("refresh-1");
        assertThat(token.getAccessToken()).isNotBlank().isNotEqualTo("access-1");
        verify(redisTemplate).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-1"));
        verify(valueOperations).set(
                eq(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", token.getAccessToken())),
                eq(onlineUser),
                eq(1800L),
                eq(TimeUnit.SECONDS)
        );
        verify(valueOperations).set(
                eq(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1")),
                eq(token.getAccessToken()),
                eq(7200L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void shouldInvalidateOnlyMatchingSessionWhenAccessTokenProvided() {
        OnlineUser onlineUser = new OnlineUser(1001L, "alice", 2001L, 3, Set.of("ROLE_ADMIN"), System.currentTimeMillis());
        when(valueOperations.get(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-1"))).thenReturn(onlineUser);
        when(setOperations.members(RedisConstants.Auth.USER_SESSION_SET.replace("{}", "1001")))
                .thenReturn(Set.of("refresh-1", "refresh-2"));
        when(valueOperations.get(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"))).thenReturn("access-1");

        redisTokenManager.invalidateToken("access-1");

        verify(redisTemplate).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-1"));
        verify(redisTemplate).delete(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-1"));
        verify(redisTemplate).delete(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"));
        verify(setOperations).remove(RedisConstants.Auth.USER_SESSION_SET.replace("{}", "1001"), "refresh-1");
        verify(redisTemplate, never()).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-2"));
        verify(redisTemplate, never()).delete(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-2"));
    }

    @Test
    void shouldInvalidateOnlyMatchingSessionWhenRefreshTokenProvided() {
        OnlineUser onlineUser = new OnlineUser(1001L, "alice", 2001L, 3, Set.of("ROLE_ADMIN"), System.currentTimeMillis());
        when(valueOperations.get(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-1"))).thenReturn(onlineUser);
        when(valueOperations.get(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"))).thenReturn("access-1");

        redisTokenManager.invalidateToken("refresh-1");

        verify(redisTemplate).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-1"));
        verify(redisTemplate).delete(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-1"));
        verify(redisTemplate).delete(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"));
        verify(setOperations).remove(RedisConstants.Auth.USER_SESSION_SET.replace("{}", "1001"), "refresh-1");
    }

    @Test
    void shouldInvalidateUserSessionsByUserId() {
        when(setOperations.members(RedisConstants.Auth.USER_SESSION_SET.replace("{}", "1001")))
                .thenReturn(Set.of("refresh-1", "refresh-2"));
        when(valueOperations.get(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"))).thenReturn("access-1");
        when(valueOperations.get(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-2"))).thenReturn("access-2");

        redisTokenManager.invalidateUserSessions(1001L);

        verify(redisTemplate).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-1"));
        verify(redisTemplate).delete(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-1"));
        verify(redisTemplate).delete(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-1"));
        verify(setOperations).remove(RedisConstants.Auth.USER_SESSION_SET.replace("{}", "1001"), "refresh-1");
        verify(redisTemplate).delete(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-2"));
        verify(redisTemplate).delete(RedisConstants.Auth.REFRESH_TOKEN_USER.replace("{}", "refresh-2"));
        verify(redisTemplate).delete(RedisConstants.Auth.SESSION_ACCESS_TOKEN.replace("{}", "refresh-2"));
        verify(setOperations).remove(RedisConstants.Auth.USER_SESSION_SET.replace("{}", "1001"), "refresh-2");
        verify(valueOperations).set(
                eq(RedisConstants.Auth.USER_SESSION_INVALID_AFTER.replace("{}", "1001")),
                anyLong(),
                eq(7200L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void shouldRejectAccessTokenAfterUserSessionInvalidated() {
        OnlineUser onlineUser = new OnlineUser(1001L, "alice", 2001L, 3, Set.of("ROLE_ADMIN"), 1000L);
        when(valueOperations.get(RedisConstants.Auth.ACCESS_TOKEN_USER.replace("{}", "access-token"))).thenReturn(onlineUser);
        when(valueOperations.get(RedisConstants.Auth.USER_SESSION_INVALID_AFTER.replace("{}", "1001"))).thenReturn(2000L);

        boolean valid = redisTokenManager.validateToken("access-token");

        assertThat(valid).isFalse();
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
