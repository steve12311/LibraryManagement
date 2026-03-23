package org.dwtech.system.service.impl;

import org.dwtech.common.token.TokenManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.dwtech.system.model.entity.UserRolePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplSessionInvalidationTest {

    @Mock
    private TokenManager tokenManager;

    private UserRoleServiceImpl userRoleService;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(UserRolePO.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "test-user-role"),
                    UserRolePO.class
            );
        }
        userRoleService = spy(new UserRoleServiceImpl(tokenManager));
    }

    @Test
    void shouldInvalidateTargetUserSessionsWhenSavingRolesChangesUserPermissions() {
        doReturn(List.of(new UserRolePO(1001L, 1L))).when(userRoleService).list(any(LambdaQueryWrapper.class));
        doReturn(true).when(userRoleService).saveBatch(any());
        doReturn(true).when(userRoleService).remove(any());

        userRoleService.saveUserRoles(1001L, List.of(2L));

        verify(tokenManager).invalidateUserSessions(1001L);
    }

    @Test
    void shouldInvalidateChangedUsersWhenAssigningUsersToRole() {
        doReturn(List.of(
                new UserRolePO(1001L, 10L),
                new UserRolePO(1002L, 10L)
        )).when(userRoleService).list(any(LambdaQueryWrapper.class));
        doReturn(true).when(userRoleService).saveBatch(any());
        doReturn(true).when(userRoleService).remove(any());

        userRoleService.assignUsersToRole(10L, List.of(1002L, 1003L));

        verify(tokenManager).invalidateUserSessions(1001L);
        verify(tokenManager).invalidateUserSessions(1003L);
        verify(tokenManager, never()).invalidateUserSessions(1002L);
    }
}
