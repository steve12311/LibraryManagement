package org.dwtech.common.token;

import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private JwtTokenManager jwtTokenManager;

    @BeforeEach
    void setUp() {
        jwtTokenManager = new JwtTokenManager(buildSecurityProperties(), redisTemplate);
    }

    @Test
    void shouldGenerateValidateAndParseToken() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        Authentication authentication = buildAuthentication();

        AuthenticationToken token = jwtTokenManager.generateToken(authentication);

        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
        assertThat(token.getExpiresIn()).isEqualTo(1800);

        assertThat(jwtTokenManager.validateToken(token.getAccessToken())).isTrue();
        assertThat(jwtTokenManager.validateRefreshToken(token.getRefreshToken())).isTrue();
        assertThat(jwtTokenManager.validateRefreshToken(token.getAccessToken())).isFalse();

        Authentication parsed = jwtTokenManager.parseToken(token.getAccessToken());
        SysUserDetails principal = (SysUserDetails) parsed.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(1001L);
        assertThat(principal.getDeptId()).isEqualTo(2001L);
        assertThat(principal.getDataScope()).isEqualTo(3);
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(parsed.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "book:view");
    }

    @Test
    void shouldReturnFalseWhenTokenIsBlacklisted() {
        AuthenticationToken token = jwtTokenManager.generateToken(buildAuthentication());
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean valid = jwtTokenManager.validateToken(token.getAccessToken());

        assertThat(valid).isFalse();
    }

    @Test
    void shouldAddTokenToBlacklistWhenInvalidateToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AuthenticationToken token = jwtTokenManager.generateToken(buildAuthentication());

        jwtTokenManager.invalidateToken("Bearer " + token.getAccessToken());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(keyCaptor.capture(), isNull(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));

        assertThat(keyCaptor.getValue()).startsWith(RedisConstants.Auth.BLACKLIST_TOKEN.replace("{}", ""));
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void shouldThrowBusinessExceptionWhenRefreshTokenInvalid() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtTokenManager.refreshToken("invalid-refresh-token")
        );

        assertThat(exception.getResultCode()).isEqualTo(ResultCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    void shouldRotateRefreshTokenWhenRefreshing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AtomicReference<String> blacklistedKey = new AtomicReference<>();
        when(redisTemplate.hasKey(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0, String.class).equals(blacklistedKey.get()));
        AuthenticationToken originalToken = jwtTokenManager.generateToken(buildAuthentication());
        doAnswer(invocation -> {
            blacklistedKey.set(invocation.getArgument(0, String.class));
            return null;
        }).when(valueOperations).set(anyString(), isNull(), anyLong(), eq(TimeUnit.SECONDS));

        AuthenticationToken refreshedToken = jwtTokenManager.refreshToken(originalToken.getRefreshToken());

        assertThat(refreshedToken.getAccessToken()).isNotBlank();
        assertThat(refreshedToken.getRefreshToken()).isNotBlank();
        assertThat(refreshedToken.getAccessToken()).isNotEqualTo(originalToken.getAccessToken());
        assertThat(refreshedToken.getRefreshToken()).isNotEqualTo(originalToken.getRefreshToken());
        assertThat(jwtTokenManager.validateRefreshToken(originalToken.getRefreshToken())).isFalse();
        assertThat(jwtTokenManager.validateRefreshToken(refreshedToken.getRefreshToken())).isTrue();
        verify(valueOperations).set(anyString(), isNull(), anyLong(), eq(TimeUnit.SECONDS));
    }

    private SecurityProperties buildSecurityProperties() {
        SecurityProperties properties = new SecurityProperties();
        SecurityProperties.SessionConfig session = new SecurityProperties.SessionConfig();
        session.setType("jwt");
        session.setAccessTokenTimeToLive(1800);
        session.setRefreshTokenTimeToLive(7200);
        SecurityProperties.JwtConfig jwtConfig = new SecurityProperties.JwtConfig();
        jwtConfig.setSecretKey("SecretKey012345678901234567890123456789");
        session.setJwt(jwtConfig);
        properties.setSession(session);
        properties.setIgnoreUrls(new String[]{"/api/v1/auth/**"});
        properties.setUnsecuredUrls(new String[]{"/doc.html"});
        return properties;
    }

    private Authentication buildAuthentication() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(1001L);
        userDetails.setDeptId(2001L);
        userDetails.setDataScope(3);
        userDetails.setUsername("alice");
        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("book:view")
        );
        userDetails.setAuthorities(authorities);
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }
}
