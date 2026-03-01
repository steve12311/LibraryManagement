package org.dwtech.common.utils;

import jakarta.servlet.http.Cookie;
import org.dwtech.common.config.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieUtilsTest {

    @Test
    void shouldWriteRefreshTokenCookieWithExpectedAttributes() {
        SecurityProperties securityProperties = buildSecurityProperties(7200);
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshTokenCookieUtils.writeRefreshTokenCookie(response, securityProperties, "rt-123");

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeader).contains("refreshToken=rt-123");
        assertThat(cookieHeader).contains("Max-Age=7200");
        assertThat(cookieHeader).contains("Path=/api/v1/auth/refresh-token");
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("Secure");
        assertThat(cookieHeader).contains("SameSite=None");
    }

    @Test
    void shouldClearRefreshTokenCookieWithZeroMaxAge() {
        SecurityProperties securityProperties = buildSecurityProperties(7200);
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshTokenCookieUtils.clearRefreshTokenCookie(response, securityProperties);

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeader).contains("refreshToken=");
        assertThat(cookieHeader).contains("Max-Age=0");
        assertThat(cookieHeader).contains("Path=/api/v1/auth/refresh-token");
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("Secure");
        assertThat(cookieHeader).contains("SameSite=None");
    }

    @Test
    void shouldGetRefreshTokenFromRequestCookie() {
        SecurityProperties securityProperties = buildSecurityProperties(7200);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "v1"), new Cookie("refreshToken", "rt-from-cookie"));

        String refreshToken = RefreshTokenCookieUtils.getRefreshToken(request, securityProperties);

        assertThat(refreshToken).isEqualTo("rt-from-cookie");
    }

    @Test
    void shouldReturnNullWhenRefreshTokenCookieMissing() {
        SecurityProperties securityProperties = buildSecurityProperties(7200);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "v1"));

        String refreshToken = RefreshTokenCookieUtils.getRefreshToken(request, securityProperties);

        assertThat(refreshToken).isNull();
    }

    @Test
    void shouldWriteCookieWhenSameSiteIsLax() {
        SecurityProperties securityProperties = buildSecurityProperties(7200);
        securityProperties.getRefreshTokenCookie().setSameSite("Lax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshTokenCookieUtils.writeRefreshTokenCookie(response, securityProperties, "rt");

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeader).contains("SameSite=Lax");
    }

    @Test
    void shouldWriteCookieWhenPathIsCustomized() {
        SecurityProperties securityProperties = buildSecurityProperties(7200);
        securityProperties.getRefreshTokenCookie().setPath("/api/v1/auth");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshTokenCookieUtils.writeRefreshTokenCookie(response, securityProperties, "rt");

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeader).contains("Path=/api/v1/auth");
    }

    private SecurityProperties buildSecurityProperties(int refreshTokenTtl) {
        SecurityProperties securityProperties = new SecurityProperties();
        SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
        sessionConfig.setRefreshTokenTimeToLive(refreshTokenTtl);
        securityProperties.setSession(sessionConfig);

        SecurityProperties.RefreshTokenCookieConfig cookieConfig = new SecurityProperties.RefreshTokenCookieConfig();
        cookieConfig.setName("refreshToken");
        cookieConfig.setPath("/api/v1/auth/refresh-token");
        cookieConfig.setHttpOnly(true);
        cookieConfig.setSecure(true);
        cookieConfig.setSameSite("None");
        securityProperties.setRefreshTokenCookie(cookieConfig);
        return securityProperties;
    }
}
