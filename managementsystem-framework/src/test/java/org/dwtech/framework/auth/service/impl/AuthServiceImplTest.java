package org.dwtech.framework.auth.service.impl;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.CaptchaProperties;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.token.TokenManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.awt.Font;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private TokenManager tokenManager;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CaptchaProperties captchaProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                tokenManager,
                new Font("Dialog", Font.PLAIN, 16),
                authenticationManager,
                codeGenerator,
                redisTemplate,
                captchaProperties
        );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldLoginSuccessfullyAndSetSecurityContext() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", null);
        AuthenticationToken token = AuthenticationToken.builder()
                .tokenType("Bearer")
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(1800)
                .build();

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenManager.generateToken(authentication)).thenReturn(token);

        AuthenticationToken actual = authService.login(" alice ", "secret");

        assertThat(actual).isEqualTo(token);
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("alice");
        assertThat(captor.getValue().getCredentials()).isEqualTo("secret");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void shouldThrowBusinessExceptionWhenBadCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login("alice", "wrong")
        );

        assertThat(exception.getResultCode()).isEqualTo(ResultCode.USER_PASSWORD_ERROR);
        assertThat(exception.getMessage()).isEqualTo("验证用户名密码失败");
    }

    @Test
    void shouldThrowBusinessExceptionWhenAuthenticationError() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new RuntimeException("boom"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login("alice", "secret")
        );

        assertThat(exception.getResultCode()).isNull();
        assertThat(exception.getMessage()).isEqualTo("登录失败，请稍后再试");
    }

    @Test
    void shouldInvalidateTokenAndClearContextOnLogout() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        authService.logout();

        verify(tokenManager).invalidateToken("access-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotInvalidateTokenWhenAuthorizationHeaderIsNotBearer() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        authService.logout();

        verify(tokenManager, never()).invalidateToken(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void shouldDelegateRefreshToken() {
        AuthenticationToken token = AuthenticationToken.builder()
                .tokenType("Bearer")
                .accessToken("new-access")
                .refreshToken("refresh")
                .expiresIn(1800)
                .build();
        when(tokenManager.refreshToken("refresh")).thenReturn(token);

        AuthenticationToken actual = authService.refreshToken("refresh");

        assertThat(actual).isEqualTo(token);
        verify(tokenManager).refreshToken("refresh");
    }

    @Test
    void shouldThrowWhenCaptchaTypeIsInvalid() {
        CaptchaProperties.CodeProperties codeProperties = new CaptchaProperties.CodeProperties();
        codeProperties.setLength(4);
        when(captchaProperties.getType()).thenReturn("invalid");
        when(captchaProperties.getWidth()).thenReturn(120);
        when(captchaProperties.getHeight()).thenReturn(40);
        when(captchaProperties.getInterfereCount()).thenReturn(2);
        when(captchaProperties.getCode()).thenReturn(codeProperties);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.getCaptcha());

        assertThat(exception.getMessage()).contains("Invalid captcha type");
    }
}
