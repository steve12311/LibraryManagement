package org.dwtech.system.service.impl;

import org.dwtech.common.constant.RedisConstants;
import org.dwtech.system.mapper.RoleMenuMapper;
import org.dwtech.system.model.bo.RolePermsBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMenuServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    private RoleMenuServiceImpl roleMenuService;

    @BeforeEach
    void setUp() {
        roleMenuService = new RoleMenuServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(roleMenuService, "baseMapper", roleMenuMapper);
    }

    @Test
    void shouldRefreshAllRolePermsCache() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        RolePermsBO admin = new RolePermsBO();
        admin.setRoleCode("ADMIN");
        admin.setPerms(Set.of("book:view", "book:add"));
        RolePermsBO guest = new RolePermsBO();
        guest.setRoleCode("GUEST");
        guest.setPerms(Set.of());

        when(hashOperations.keys(RedisConstants.System.ROLE_PERMS)).thenReturn(Set.of("OLD_ADMIN", "OLD_GUEST"));
        when(roleMenuMapper.getRolePermsList(null)).thenReturn(List.of(admin, guest));

        roleMenuService.refreshRolePermsCache();

        ArgumentCaptor<Object[]> deleteCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(hashOperations).delete(eq(RedisConstants.System.ROLE_PERMS), deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactlyInAnyOrder("OLD_ADMIN", "OLD_GUEST");
        verify(hashOperations).put(RedisConstants.System.ROLE_PERMS, "ADMIN", admin.getPerms());
        verify(hashOperations, never()).put(eq(RedisConstants.System.ROLE_PERMS), eq("GUEST"), any());
    }

    @Test
    void shouldRefreshSpecificRolePermsCache() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        RolePermsBO admin = new RolePermsBO();
        admin.setRoleCode("ADMIN");
        admin.setPerms(Set.of("book:view"));
        when(roleMenuMapper.getRolePermsList("ADMIN")).thenReturn(List.of(admin));

        roleMenuService.refreshRolePermsCache("ADMIN");

        verify(hashOperations).delete(RedisConstants.System.ROLE_PERMS, "ADMIN");
        verify(hashOperations).put(RedisConstants.System.ROLE_PERMS, "ADMIN", admin.getPerms());
    }

    @Test
    void shouldOnlyDeleteWhenSpecificRoleHasNoPerms() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(roleMenuMapper.getRolePermsList("ADMIN")).thenReturn(List.of());

        roleMenuService.refreshRolePermsCache("ADMIN");

        verify(hashOperations).delete(RedisConstants.System.ROLE_PERMS, "ADMIN");
        verify(hashOperations, never()).put(any(), any(), any());
    }

    @Test
    void shouldRefreshRolePermsCacheWhenRoleCodeChanged() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        RolePermsBO newRole = new RolePermsBO();
        newRole.setRoleCode("LIBRARIAN");
        newRole.setPerms(Set.of("book:borrow"));
        when(roleMenuMapper.getRolePermsList("LIBRARIAN")).thenReturn(List.of(newRole));

        roleMenuService.refreshRolePermsCache("ADMIN", "LIBRARIAN");

        verify(hashOperations).delete(RedisConstants.System.ROLE_PERMS, "ADMIN");
        verify(hashOperations).put(RedisConstants.System.ROLE_PERMS, "LIBRARIAN", newRole.getPerms());
    }

    @Test
    void shouldReturnMenuIdsByRoleId() {
        when(roleMenuMapper.listMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));

        List<Long> menuIds = roleMenuService.listMenuIdsByRoleId(1L);

        assertThat(menuIds).containsExactly(10L, 20L);
    }

    @Test
    void shouldReturnPermsByRoleCodes() {
        Set<String> roles = Set.of("ADMIN", "LIBRARIAN");
        Set<String> perms = Set.of("book:view", "book:add");
        when(roleMenuMapper.listRolePerms(roles)).thenReturn(perms);

        Set<String> actual = roleMenuService.getRolePermsByRoleCodes(roles);

        assertThat(actual).isEqualTo(perms);
    }
}
