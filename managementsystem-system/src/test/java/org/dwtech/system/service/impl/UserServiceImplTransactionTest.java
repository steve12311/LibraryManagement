package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTransactionTest {

    @Mock
    private RoleService roleService;

    @Mock
    private org.dwtech.system.converter.UserConverter userConverter;

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
        if (TableInfoHelper.getTableInfo(org.dwtech.system.model.entity.UserPO.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "test-user-transaction"),
                    org.dwtech.system.model.entity.UserPO.class
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
    }

    @Test
    void shouldDeclareSaveUserTransactional() throws NoSuchMethodException {
        Transactional transactional = UserServiceImpl.class
                .getMethod("saveUser", UserForm.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    @Test
    void shouldDeclareDeleteUsersTransactional() throws NoSuchMethodException {
        Transactional transactional = UserServiceImpl.class
                .getMethod("deleteUsers", List.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    @Test
    void shouldRemoveUserRolesAfterDeletingUsers() {
        List<Long> userIds = List.of(1001L, 1002L);
        doReturn(true).when(userService).removeByIds(userIds);

        boolean result = userService.deleteUsers(userIds);

        assertTrue(result);
        InOrder inOrder = inOrder(userService, userRoleService);
        inOrder.verify(userService).removeByIds(userIds);
        inOrder.verify(userRoleService).removeUserRolesByUserIds(userIds);
    }

    @Test
    void shouldStopDeletingWhenRemoveByIdsFails() {
        List<Long> userIds = List.of(1001L, 1002L);
        doReturn(false).when(userService).removeByIds(userIds);

        assertThrows(IllegalArgumentException.class, () -> userService.deleteUsers(userIds));
        verify(userRoleService, never()).removeUserRolesByUserIds(anyList());
    }
}
