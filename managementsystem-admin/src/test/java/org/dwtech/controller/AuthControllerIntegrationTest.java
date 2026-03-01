package org.dwtech.controller;

import jakarta.servlet.http.Cookie;
import org.dwtech.auth.model.form.UserLoginForm;
import org.dwtech.auth.model.vo.CaptchaVO;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.core.entity.AuthenticationToken;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.framework.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SecurityProperties securityProperties;

    @BeforeEach
    void initSecurityProperties() {
        SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
        sessionConfig.setRefreshTokenTimeToLive(604800);

        SecurityProperties.RefreshTokenCookieConfig cookieConfig = new SecurityProperties.RefreshTokenCookieConfig();
        cookieConfig.setName("refreshToken");
        cookieConfig.setPath("/api/v1/auth/refresh-token");
        cookieConfig.setHttpOnly(true);
        cookieConfig.setSecure(true);
        cookieConfig.setSameSite("None");

        when(securityProperties.getSession()).thenReturn(sessionConfig);
        when(securityProperties.getRefreshTokenCookie()).thenReturn(cookieConfig);
    }

    @Test
    void shouldGetCaptchaSuccessfully() throws Exception {
        CaptchaVO captchaVO = CaptchaVO.builder()
                .captchaKey("captcha-key")
                .captchaBase64("data:image/png;base64,abcd")
                .build();
        when(authService.getCaptcha()).thenReturn(captchaVO);

        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value(ResultCode.SUCCESS.getMsg()))
                .andExpect(jsonPath("$.data.captchaKey").value("captcha-key"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        UserLoginForm form = new UserLoginForm();
        form.setUsername("alice");
        form.setPassword("secret");
        AuthenticationToken token = AuthenticationToken.builder()
                .tokenType("Bearer")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(1800)
                .build();
        when(authService.login("alice", "secret")).thenReturn(token);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", form.getUsername())
                        .param("password", form.getPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken=refresh-token"),
                        containsString("Max-Age=604800"),
                        containsString("HttpOnly"),
                        containsString("Secure"),
                        containsString("SameSite=None"),
                        containsString("Path=/api/v1/auth/refresh-token")
                )))
                .andExpect(jsonPath("$.data.expiresIn").value(1800));

        verify(authService).login("alice", "secret");
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        AuthenticationToken token = AuthenticationToken.builder()
                .tokenType("Bearer")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(1800)
                .build();
        when(authService.refreshToken("refresh-token")).thenReturn(token);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken=new-refresh-token"),
                        containsString("Max-Age=604800"),
                        containsString("HttpOnly"),
                        containsString("Secure"),
                        containsString("SameSite=None"),
                        containsString("Path=/api/v1/auth/refresh-token")
                )));

        verify(authService).refreshToken("refresh-token");
    }

    @Test
    void shouldFailWhenRefreshTokenCookieMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.msg").value(ResultCode.REFRESH_TOKEN_INVALID.getMsg()));

        verify(authService, never()).refreshToken(anyString());
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value(ResultCode.SUCCESS.getMsg()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken="),
                        containsString("Max-Age=0"),
                        containsString("HttpOnly"),
                        containsString("Secure"),
                        containsString("SameSite=None"),
                        containsString("Path=/api/v1/auth/refresh-token")
                )));

        verify(authService).logout();
    }
}
