package org.dwtech.controller;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.controller.lib.BorrowController;
import org.dwtech.framework.config.SecurityConfig;
import org.dwtech.framework.config.WebMvcConfig;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.system.service.BorrowService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BorrowController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfig.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, BorrowControllerIntegrationTest.TestSecurityPropertiesConfig.class})
class BorrowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowService borrowService;

    @Autowired
    @Qualifier("ss")
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
    void shouldRejectAnonymousSaveBorrow() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "9787300000001",
                                  "userId": 2002
                                }
                                """)))
                .hasCauseInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(borrowService);
    }

    @Test
    void shouldRejectAuthenticatedUserWithoutBorrowAddPermission() throws Exception {
        authenticateAsLibrarian();
        when(permissionService.hasPerm("lib:borrow:add")).thenReturn(false);

        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "9787300000001",
                                  "userId": 2002
                                }
                                """)))
                .hasCauseInstanceOf(AuthorizationDeniedException.class);

        verify(permissionService, atLeastOnce()).hasPerm("lib:borrow:add");
        verifyNoInteractions(borrowService);
    }

    @Test
    void shouldAllowAuthenticatedLibrarianToSaveBorrow() throws Exception {
        authenticateAsLibrarian();
        when(permissionService.hasPerm("lib:borrow:add")).thenReturn(true);
        when(borrowService.saveBorrow(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "9787300000001",
                                  "userId": 2002
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        verify(permissionService, atLeastOnce()).hasPerm("lib:borrow:add");
        verify(borrowService).saveBorrow(any());
    }

    @Test
    void shouldAllowAuthenticatedLibrarianToUpdateBorrow() throws Exception {
        authenticateAsLibrarian();
        when(permissionService.hasPerm("lib:borrow:edit")).thenReturn(true);
        when(borrowService.updateBorrow(any(), any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/borrow/borrow-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        verify(permissionService, atLeastOnce()).hasPerm("lib:borrow:edit");
        verify(borrowService).updateBorrow(any(), any());
    }

    private void authenticateAsLibrarian() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("librarian", null, List.of())
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
