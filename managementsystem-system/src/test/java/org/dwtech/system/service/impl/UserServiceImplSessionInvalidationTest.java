package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.dwtech.common.token.TokenManager;
import org.dwtech.common.service.PermissionService;
import org.dwtech.system.converter.UserConverter;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.file.queue.FileRefCountDeletePublisher;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSessionInvalidationTest {

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
                    new MapperBuilderAssistant(new MybatisConfiguration(), "test-user"),
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
    void shouldInvalidateTargetUserSessionsWhenResetPasswordSucceeds() {
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        doReturn(true).when(userService).update(any());

        userService.resetPassword(1001L, "new-password");

        verify(tokenManager).invalidateUserSessions(1001L);
    }

    @Test
    void shouldInvalidateTargetUserSessionsWhenStatusUpdated() {
        doReturn(true).when(userService).update(any());

        userService.updateUserStatus(1001L, 0);

        verify(tokenManager).invalidateUserSessions(1001L);
    }

    @Test
    void shouldInvalidateUserSessionsWhenChangingPassword() {
        UserPO user = new UserPO();
        user.setId(1001L);
        user.setPassword("old-hash");
        PasswordUpdateForm form = new PasswordUpdateForm();
        form.setOldPassword("old-password");
        form.setNewPassword("new-password");
        form.setConfirmPassword("new-password");
        doReturn(user).when(userService).getById(1001L);
        doReturn(true).when(userService).update(any());
        when(passwordEncoder.matches(eq("old-password"), eq("old-hash"))).thenReturn(true);
        when(passwordEncoder.matches(eq("new-password"), eq("old-hash"))).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        userService.changePassword(1001L, form);

        verify(tokenManager).invalidateUserSessions(1001L);
    }
}
