package org.dwtech.controller;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.controller.sys.UserController;
import org.dwtech.framework.config.SecurityConfig;
import org.dwtech.framework.config.WebMvcConfig;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.system.model.dto.UserExportDTO;
import org.dwtech.system.model.vo.UserImportResultVO;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfig.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, UserImportExportControllerIntegrationTest.TestSecurityPropertiesConfig.class})
class UserImportExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BorrowService borrowService;

    @MockitoBean(name = "ss")
    private PermissionService permissionService;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectAnonymousDownloadTemplate() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/template")))
                .hasCauseInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void shouldAllowTemplateDownloadWithAddPermission() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("sys:user:add")).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));

        verify(permissionService).hasPerm("sys:user:add");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectImportWhenMissingAddPermission() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("sys:user:add")).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> mockMvc.perform(multipart("/api/v1/users/import").file(file)))
                .hasCauseInstanceOf(AuthorizationDeniedException.class);

        verify(permissionService).hasPerm("sys:user:add");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldImportUsersWhenHasAddPermission() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("sys:user:add")).thenReturn(true);

        UserImportResultVO result = new UserImportResultVO();
        result.setTotalCount(2);
        result.setSuccessCount(2);
        result.setFailureCount(0);
        when(userService.importUsers(any())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failureCount").value(0));

        verify(userService).importUsers(any());
    }

    @Test
    void shouldRejectExportWhenMissingListPermission() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("sys:user:list")).thenReturn(false);

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/export")))
                .hasCauseInstanceOf(AuthorizationDeniedException.class);

        verify(permissionService).hasPerm("sys:user:list");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldExportUsersWhenHasListPermission() throws Exception {
        authenticateCurrentUser();
        when(permissionService.hasPerm("sys:user:list")).thenReturn(true);

        UserExportDTO exportDTO = new UserExportDTO();
        exportDTO.setUsername("reader01");
        exportDTO.setNickname("读者一号");
        exportDTO.setRoleNames("读者");
        exportDTO.setGenderLabel("男");
        exportDTO.setStatusLabel("启用");
        exportDTO.setCreateTime(LocalDateTime.of(2026, 4, 20, 10, 30));
        when(userService.listExportUsers(any())).thenReturn(List.of(exportDTO));

        mockMvc.perform(get("/api/v1/users/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));

        verify(userService).listExportUsers(any());
    }

    private void authenticateCurrentUser() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(1001L);
        userDetails.setUsername("admin01");
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @TestConfiguration
    static class TestSecurityPropertiesConfig {

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties securityProperties = new SecurityProperties();

            SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
            sessionConfig.setType("jwt");
            sessionConfig.setRefreshTokenTimeToLive(604800);
            securityProperties.setSession(sessionConfig);

            SecurityProperties.RefreshTokenCookieConfig cookieConfig = new SecurityProperties.RefreshTokenCookieConfig();
            cookieConfig.setName("refreshToken");
            cookieConfig.setPath("/api/v1/auth/refresh-token");
            cookieConfig.setHttpOnly(true);
            cookieConfig.setSecure(false);
            cookieConfig.setSameSite("Lax");
            securityProperties.setRefreshTokenCookie(cookieConfig);

            SecurityProperties.CorsConfig corsConfig = new SecurityProperties.CorsConfig();
            securityProperties.setCors(corsConfig);
            securityProperties.setIgnoreUrls(new String[]{"/api/v1/auth/**"});
            securityProperties.setUnsecuredUrls(new String[]{"/doc.html"});
            return securityProperties;
        }
    }
}
