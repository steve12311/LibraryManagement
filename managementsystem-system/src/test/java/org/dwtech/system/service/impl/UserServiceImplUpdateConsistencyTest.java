package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.system.file.queue.FileRefCountDeletePublisher;
import org.dwtech.system.converter.UserConverter;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUpdateConsistencyTest {

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
    private FileRefCountDeletePublisher fileRefCountDeletePublisher;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(UserPO.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "test-user-update"),
                    UserPO.class
            );
        }
        userService = spy(new UserServiceImpl(
                roleService,
                userConverter,
                passwordEncoder,
                userRoleService,
                permissionService,
                tokenManager,
                fileRefCountDeletePublisher
        ));
    }

    @Test
    void shouldUsePathUserIdWhenUpdatingUserAndSavingRoles() {
        Long pathUserId = 1001L;
        UserForm form = buildUserForm(2002L, 1, List.of(1L, 2L));
        UserPO currentUser = new UserPO();
        currentUser.setId(pathUserId);
        currentUser.setStatus(1);
        UserPO mappedUser = new UserPO();
        mappedUser.setId(2002L);
        mappedUser.setStatus(1);

        doReturn(currentUser).when(userService).getById(pathUserId);
        doReturn(0L).when(userService).count(any());
        when(userConverter.toPo(form)).thenReturn(mappedUser);
        doReturn(true).when(userService).updateById(any(UserPO.class));

        boolean result = userService.updateUser(pathUserId, form);

        assertTrue(result);
        verify(userService).updateById(argThat(user -> pathUserId.equals(user.getId())));
        verify(userRoleService).saveUserRoles(pathUserId, form.getRoleIds());
        verify(tokenManager, never()).invalidateUserSessions(anyLong());
    }

    @Test
    void shouldInvalidateTargetUserSessionsWhenStatusChangesDuringUpdate() {
        Long pathUserId = 1001L;
        UserForm form = buildUserForm(pathUserId, 0, List.of(3L));
        UserPO currentUser = new UserPO();
        currentUser.setId(pathUserId);
        currentUser.setStatus(1);
        UserPO mappedUser = new UserPO();
        mappedUser.setId(pathUserId);
        mappedUser.setStatus(0);

        doReturn(currentUser).when(userService).getById(pathUserId);
        doReturn(0L).when(userService).count(any());
        when(userConverter.toPo(form)).thenReturn(mappedUser);
        doReturn(true).when(userService).updateById(any(UserPO.class));

        boolean result = userService.updateUser(pathUserId, form);

        assertTrue(result);
        verify(tokenManager).invalidateUserSessions(pathUserId);
        verify(userRoleService).saveUserRoles(pathUserId, form.getRoleIds());
    }

    private UserForm buildUserForm(Long bodyUserId, Integer status, List<Long> roleIds) {
        UserForm form = new UserForm();
        form.setId(bodyUserId);
        form.setUsername("test-user");
        form.setNickname("测试用户");
        form.setStatus(status);
        form.setRoleIds(roleIds);
        return form;
    }
}
