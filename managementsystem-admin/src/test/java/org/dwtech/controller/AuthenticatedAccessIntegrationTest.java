package org.dwtech.controller;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.controller.sys.MenuController;
import org.dwtech.controller.sys.UserController;
import org.dwtech.framework.config.SecurityConfig;
import org.dwtech.framework.config.WebMvcConfig;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.vo.MyBorrowPageVO;
import org.dwtech.system.model.vo.RouteVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.MenuService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {UserController.class, MenuController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfig.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, AuthenticatedAccessIntegrationTest.TestSecurityPropertiesConfig.class})
class AuthenticatedAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BorrowService borrowService;

    @MockitoBean
    private MenuService menuService;

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
    void shouldRejectAnonymousGetCurrentUser() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/me")))
                .hasCauseInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void shouldAllowAuthenticatedGetCurrentUser() throws Exception {
        authenticateCurrentUser(1001L, "reader01");

        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setUserId(1001L);
        currentUserVO.setUsername("reader01");
        when(userService.getCurrentUserInfo()).thenReturn(currentUserVO);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.username").value("reader01"));

        verify(userService).getCurrentUserInfo();
    }

    @Test
    void shouldRejectAnonymousGetCurrentUserBorrows() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/users/me/borrows/page")))
                .hasCauseInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(borrowService);
    }

    @Test
    void shouldAllowAuthenticatedGetCurrentUserBorrows() throws Exception {
        authenticateCurrentUser(1001L, "reader01");

        MyBorrowPageVO borrowPageVO = new MyBorrowPageVO();
        borrowPageVO.setBorrowId("borrow-1");
        borrowPageVO.setIsbn("9787300000001");
        borrowPageVO.setCover("/api/v1/files/12");
        borrowPageVO.setBookName("Spring Boot 实战");
        borrowPageVO.setStatus(1);

        Page<MyBorrowPageVO> borrowPage = new Page<>(1, 10);
        borrowPage.setRecords(List.of(borrowPageVO));
        borrowPage.setTotal(1);
        when(borrowService.getCurrentUserBorrowPage(any(), any())).thenReturn(borrowPage);

        mockMvc.perform(get("/api/v1/users/me/borrows/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.list[0].borrowId").value("borrow-1"))
                .andExpect(jsonPath("$.data.list[0].isbn").value("9787300000001"))
                .andExpect(jsonPath("$.data.list[0].cover").value("/api/v1/files/12"))
                .andExpect(jsonPath("$.data.list[0].bookName").value("Spring Boot 实战"))
                .andExpect(jsonPath("$.data.list[0].status").value(1))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(borrowService).getCurrentUserBorrowPage(any(), any());
    }

    @Test
    void shouldRejectCurrentUserBorrowStatusOutsideRange() throws Exception {
        authenticateCurrentUser(1001L, "reader01");

        mockMvc.perform(get("/api/v1/users/me/borrows/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_USER_INPUT.getCode()))
                .andExpect(jsonPath("$.msg").value("状态值不合法"));

        verifyNoInteractions(borrowService);
    }

    @Test
    void shouldAllowAuthenticatedGetUserProfile() throws Exception {
        authenticateCurrentUser(1001L, "reader01");

        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setId(1001L);
        userProfileVO.setUsername("reader01");
        userProfileVO.setNickname("读者一号");
        when(userService.getUserProfile(1001L)).thenReturn(userProfileVO);

        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.nickname").value("读者一号"));

        verify(userService).getUserProfile(1001L);
    }

    @Test
    void shouldAllowAuthenticatedUpdatePassword() throws Exception {
        authenticateCurrentUser(1001L, "reader01");
        when(userService.changePassword(any(), any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "old-pass",
                                  "newPassword": "new-pass",
                                  "confirmPassword": "new-pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        verify(userService).changePassword(any(), any());
    }

    @Test
    void shouldAllowAuthenticatedUpdateUserProfile() throws Exception {
        authenticateCurrentUser(1001L, "reader01");
        when(userService.updateUserProfile(any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "新昵称"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        verify(userService).updateUserProfile(any());
    }

    @Test
    void shouldRejectAnonymousGetCurrentUserRoutes() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/menus/routes")))
                .hasCauseInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(menuService);
    }

    @Test
    void shouldAllowAuthenticatedGetCurrentUserRoutes() throws Exception {
        authenticateCurrentUser(1001L, "reader01");

        RouteVO routeVO = new RouteVO();
        routeVO.setPath("/dashboard");
        when(menuService.listCurrentUserRoutes()).thenReturn(List.of(routeVO));

        mockMvc.perform(get("/api/v1/menus/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].path").value("/dashboard"));

        verify(menuService).listCurrentUserRoutes();
    }

    private void authenticateCurrentUser(Long userId, String username) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(userId);
        userDetails.setUsername(username);
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
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
