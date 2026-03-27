package org.dwtech.controller;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.framework.config.SecurityConfig;
import org.dwtech.framework.config.WebMvcConfig;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.system.model.vo.DashboardOverviewVO;
import org.dwtech.system.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DashboardController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfig.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, DashboardControllerIntegrationTest.TestSecurityPropertiesConfig.class})
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private TokenManager tokenManager;

    @MockitoBean
    private SysUserDetailService sysUserDetailService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CodeGenerator codeGenerator;

    @Autowired
    @Qualifier("ss")
    private PermissionService permissionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectAnonymousDashboardOverview() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/dashboard/overview")))
                .hasCauseInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(dashboardService);
    }

    @Test
    void shouldAllowDashboardOverviewWhenAuthenticatedAndPermitted() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("dashboard:view")).thenReturn(true);
        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setBookTotal(10L);
        overview.setStockTotal(20L);
        when(dashboardService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.bookTotal").value(10))
                .andExpect(jsonPath("$.data.stockTotal").value(20));

        verify(dashboardService).getOverview();
    }

    @Test
    void shouldRejectInvalidTrendWindow() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("dashboard:view")).thenReturn(true);

        mockMvc.perform(get("/api/v1/dashboard/trends")
                        .param("mode", "day")
                        .param("days", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_USER_INPUT.getCode()))
                .andExpect(jsonPath("$.msg", containsString("按天模式仅支持 7 天或 30 天")));

        verifyNoInteractions(dashboardService);
    }

    @Test
    void shouldRejectRecentEventsPageSizeOutsideBoundary() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("dashboard:view")).thenReturn(true);

        mockMvc.perform(get("/api/v1/dashboard/recent-events")
                        .param("borrowPageSize", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_USER_INPUT.getCode()))
                .andExpect(jsonPath("$.msg", containsString("借阅每页条数不能超过 20")));

        verifyNoInteractions(dashboardService);
    }

    private void authenticateCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null)
        );
    }

    @TestConfiguration
    static class TestSecurityPropertiesConfig {

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties securityProperties = new SecurityProperties();

            SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
            sessionConfig.setType("jwt");
            securityProperties.setSession(sessionConfig);

            SecurityProperties.CorsConfig corsConfig = new SecurityProperties.CorsConfig();
            securityProperties.setCors(corsConfig);
            securityProperties.setIgnoreUrls(new String[]{"/api/v1/auth/**", "/api/v1/index/books/page"});
            securityProperties.setUnsecuredUrls(new String[]{"/doc.html"});
            return securityProperties;
        }

        @Bean("ss")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
