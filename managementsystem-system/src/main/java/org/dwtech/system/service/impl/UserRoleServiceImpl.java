package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.system.model.entity.UserRolePO;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.UserRoleMapper;
import org.dwtech.system.service.UserRoleService;
import org.dwtech.common.token.TokenManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * UserRoleServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRolePO> implements UserRoleService {
    private final TokenManager tokenManager;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return;
        }

        if (CollectionUtil.isEmpty(roleIds)) {
            this.remove(new LambdaQueryWrapper<UserRolePO>()
                    .eq(UserRolePO::getUserId, userId));
            return;
        }

        // 获取现有角色
        List<Long> userRoleIds = this.list(new LambdaQueryWrapper<UserRolePO>()
                        .select(UserRolePO::getRoleId)
                        .eq(UserRolePO::getUserId, userId))
                .parallelStream()
                .map(UserRolePO::getRoleId)
                .toList();

        // 使用Set提升对比效率
        Set<Long> oldRoles = new HashSet<>(userRoleIds);
        Set<Long> newRoles = new HashSet<>(roleIds);

        // 计算变更集
        Set<Long> addedRoles = new HashSet<>(newRoles);
        addedRoles.removeAll(oldRoles);

        Set<Long> removedRoles = new HashSet<>(oldRoles);
        removedRoles.removeAll(newRoles);

        boolean rolesChanged = !addedRoles.isEmpty() || !removedRoles.isEmpty();

        // 批量保存新增角色
        if (!addedRoles.isEmpty()) {
            this.saveBatch(addedRoles.stream()
                    .map(roleId -> new UserRolePO(userId, roleId))
                    .collect(Collectors.toList()));
        }

        // 删除废弃角色
        if (!removedRoles.isEmpty()) {
            this.remove(new LambdaQueryWrapper<UserRolePO>()
                    .eq(UserRolePO::getUserId, userId)
                    .in(UserRolePO::getRoleId, removedRoles));
        }

        // 当权限变更时清除登录态
        if (rolesChanged) {
            // 获取用户所有有效token（根据实际token存储实现）
            String accessToken = SecurityUtils.getTokenFromRequest();
            tokenManager.invalidateToken(accessToken);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public void assignUsersToRole(Long roleId, List<Long> userIds) {
        if (roleId == null) {
            return;
        }

        // 先清空该角色已有的用户关联
        this.remove(new LambdaQueryWrapper<UserRolePO>()
                .eq(UserRolePO::getRoleId, roleId)
        );

        if (CollectionUtil.isEmpty(userIds)) {
            return;
        }

        // 重新写入用户与角色映射
        this.saveBatch(userIds.stream()
                .distinct()
                .map(userId -> new UserRolePO(userId, roleId))
                .toList());
    }

    @Override
    public boolean hasAssignedUsers(Long roleId) {
        int count = this.baseMapper.countUsersForRole(roleId);
        return count > 0;
    }
}
