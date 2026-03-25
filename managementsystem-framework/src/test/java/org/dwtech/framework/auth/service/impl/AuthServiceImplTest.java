package org.dwtech.framework.auth.service.impl;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.CaptchaProperties;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.core.entity.SysUserDetails;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import java.awt.Font;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
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

    private SecurityProperties securityProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        SecurityProperties.RefreshTokenCookieConfig refreshTokenCookieConfig =
                new SecurityProperties.RefreshTokenCookieConfig();
        refreshTokenCookieConfig.setName("refreshToken");
        securityProperties.setRefreshTokenCookie(refreshTokenCookieConfig);
        authService = new AuthServiceImpl(
                tokenManager,
                new Font("Dialog", Font.PLAIN, 16),
                authenticationManager,
                codeGenerator,
                redisTemplate,
                captchaProperties,
                securityProperties
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
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
    void shouldThrowBusinessExceptionWhenAccountDisabled() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new DisabledException("disabled"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login("alice", "secret")
        );

        assertThat(exception.getResultCode()).isEqualTo(ResultCode.USER_LOGIN_EXCEPTION);
        assertThat(exception.getMessage()).isEqualTo("账号已禁用");
    }

    @Test
    void shouldLogStructuredFailureWithoutLeakingExceptionMessageOnBadCredentials(CapturedOutput output) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials secret"));

        assertThrows(BusinessException.class, () -> authService.login("alice", "wrong"));

        assertThat(output).contains("action=login");
        assertThat(output).contains("username=alice");
        assertThat(output).contains(ResultCode.USER_PASSWORD_ERROR.getCode());
        assertThat(output).doesNotContain("bad credentials secret");
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
    void shouldInvalidateAccessAndRefreshTokenOnLogout() {
        SecurityContextHolder.getContext().setAuthentication(buildAuthentication());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        request.setCookies(new Cookie("refreshToken", "refresh-token"));
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        authService.logout();

        verify(tokenManager).invalidateToken("access-token");
        verify(tokenManager).invalidateToken("refresh-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldInvalidateRefreshTokenEvenWithoutBearerAccessToken(CapturedOutput output) {
        SecurityContextHolder.getContext().setAuthentication(buildAuthentication());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token");
        request.setCookies(new Cookie("refreshToken", "refresh-secret-token"));
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        authService.logout();

        verify(tokenManager, never()).invalidateToken("access-token");
        verify(tokenManager).invalidateToken("refresh-secret-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(output).contains("action=logout");
        assertThat(output).contains("hadRefreshToken=true");
        assertThat(output).doesNotContain("refresh-secret-token");
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
    void shouldLogStructuredRefreshFailureWithoutLeakingRefreshToken(CapturedOutput output) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(tokenManager.refreshToken("refresh-secret-token"))
                .thenThrow(new BusinessException(ResultCode.REFRESH_TOKEN_INVALID));

        assertThrows(BusinessException.class, () -> authService.refreshToken("refresh-secret-token"));

        assertThat(output).contains("action=refresh_token");
        assertThat(output).contains(ResultCode.REFRESH_TOKEN_INVALID.getCode());
        assertThat(output).doesNotContain("refresh-secret-token");
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

    private Authentication buildAuthentication() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(1001L);
        userDetails.setUsername("alice");
        userDetails.setEnabled(true);
        userDetails.setAuthorities(java.util.Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
