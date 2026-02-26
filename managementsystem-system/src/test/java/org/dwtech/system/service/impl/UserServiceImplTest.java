package org.dwtech.system.service.impl;

import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.system.converter.UserConverter;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

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

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = spy(new UserServiceImpl(
                roleService,
                userConverter,
                passwordEncoder,
                userRoleService,
                permissionService,
                tokenManager
        ));
    }

    @Test
    void shouldRejectWhenConfirmPasswordNotEqual() {
        UserPO user = new UserPO();
        user.setId(1L);
        user.setPassword("encoded-old-password");
        doReturn(user).when(userService).getById(1L);

        when(passwordEncoder.matches("old-password", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "encoded-old-password")).thenReturn(false);

        PasswordUpdateForm form = new PasswordUpdateForm();
        form.setOldPassword("old-password");
        form.setNewPassword("new-password");
        form.setConfirmPassword("other-password");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, form));

        assertThat(exception.getMessage()).isEqualTo("新密码和确认密码不一致");
    }
}
