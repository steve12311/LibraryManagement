package org.dwtech.system.service.impl;

import cn.idev.excel.EasyExcel;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.model.Option;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.system.converter.UserConverter;
import org.dwtech.system.mapper.UserMapper;
import org.dwtech.system.model.bo.UserBO;
import org.dwtech.system.model.dto.UserExportDTO;
import org.dwtech.system.model.dto.UserImportDTO;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.entity.UserRolePO;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.UserImportResultVO;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplImportExportTest {

    @Mock
    private RoleService roleService;

    @Mock
    private UserConverter userConverter;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private TokenManager tokenManager;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(UserPO.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "test-user-import-export"),
                    UserPO.class
            );
        }

        userService = spy(new UserServiceImpl(
                roleService,
                userConverter,
                passwordEncoder,
                userRoleService,
                permissionService,
                tokenManager
        ));
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        authenticateCurrentUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldImportUsersWithBatchSizeHundred() throws Exception {
        when(roleService.listRoleOptions()).thenReturn(List.of(new Option<>(11L, "读者")));
        when(passwordEncoder.encode("123456")).thenReturn("ENCODED");
        doReturn(List.of()).doReturn(List.of(
                buildUserIdOnly("reader01", 1001L),
                buildUserIdOnly("reader02", 1002L)
        )).when(userService).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        doReturn(true).when(userService).saveBatch(any(), eq(100));

        InputStream inputStream = buildImportExcel(List.of(
                buildImportRow("reader01", "读者一号", "男", "13800138000", "reader01@test.com", "读者"),
                buildImportRow("reader02", "读者二号", "女", "13800138001", "reader02@test.com", "读者")
        ));

        UserImportResultVO result = userService.importUsers(inputStream);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getMessages()).isEmpty();

        verify(userService).saveBatch(argThat(users -> users.size() == 2), eq(100));
        verify(userRoleService).saveBatchUserRoles(argThat(userRoles ->
                userRoles.size() == 2
                        && userRoles.stream().map(UserRolePO::getRoleId).allMatch(roleId -> roleId.equals(11L))
        ), eq(100));
    }

    @Test
    void shouldSkipRowsWhenUsernameAlreadyExists() throws Exception {
        when(roleService.listRoleOptions()).thenReturn(List.of(new Option<>(11L, "读者")));
        doReturn(List.of(buildUserIdOnly("reader01", 1L)))
                .when(userService)
                .list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        InputStream inputStream = buildImportExcel(List.of(
                buildImportRow("reader01", "读者一号", "男", "13800138000", "reader01@test.com", "读者")
        ));

        UserImportResultVO result = userService.importUsers(inputStream);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getMessages()).singleElement().asString().contains("用户名已存在");
        verify(userService, never()).saveBatch(any(), eq(100));
        verify(userRoleService, never()).saveBatchUserRoles(any(), eq(100));
    }

    @Test
    void shouldSkipRowsWhenRoleNameInvalid() throws Exception {
        when(roleService.listRoleOptions()).thenReturn(List.of(new Option<>(11L, "读者")));
        doReturn(List.of()).when(userService).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        InputStream inputStream = buildImportExcel(List.of(
                buildImportRow("reader01", "读者一号", "男", "13800138000", "reader01@test.com", "不存在的角色")
        ));

        UserImportResultVO result = userService.importUsers(inputStream);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getMessages()).singleElement().asString().contains("不存在或不可分配");
        verify(userService, never()).saveBatch(any(), eq(100));
    }

    @Test
    void shouldConvertExportUsersToReadableLabels() {
        authenticateRootUser();
        UserPageQuery query = new UserPageQuery();

        UserBO user = new UserBO();
        user.setUsername("reader01");
        user.setNickname("读者一号");
        user.setRoleNames("读者");
        user.setGender(1);
        user.setMobile("13800138000");
        user.setEmail("reader01@test.com");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.of(2026, 4, 20, 10, 30));
        UserExportDTO exportDto = new UserExportDTO();
        exportDto.setUsername("reader01");
        exportDto.setGenderLabel("男");
        exportDto.setStatusLabel("启用");
        exportDto.setRoleNames("读者");
        when(userMapper.listExportUsers(query)).thenReturn(List.of(user));
        when(userConverter.toExportDtos(List.of(user))).thenReturn(List.of(exportDto));

        List<UserExportDTO> exportUsers = userService.listExportUsers(query);

        assertThat(query.getIsRoot()).isTrue();
        assertThat(exportUsers).containsExactly(exportDto);
        verify(userMapper).listExportUsers(query);
        verify(userConverter).toExportDtos(List.of(user));
    }

    private InputStream buildImportExcel(List<UserImportDTO> rows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream, UserImportDTO.class)
                .sheet("用户导入模板")
                .doWrite(rows);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private UserImportDTO buildImportRow(
            String username,
            String nickname,
            String genderLabel,
            String mobile,
            String email,
            String roleNames
    ) {
        UserImportDTO row = new UserImportDTO();
        row.setUsername(username);
        row.setNickname(nickname);
        row.setGenderLabel(genderLabel);
        row.setMobile(mobile);
        row.setEmail(email);
        row.setRoleNames(roleNames);
        return row;
    }

    private UserPO buildUserIdOnly(String username, Long userId) {
        UserPO user = new UserPO();
        user.setUsername(username);
        user.setId(userId);
        return user;
    }

    private void authenticateCurrentUser() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(9001L);
        userDetails.setUsername("operator");
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private void authenticateRootUser() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(1L);
        userDetails.setUsername("root");
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_ROOT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}
