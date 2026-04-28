package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.system.model.entity.UserRolePO;
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
 * 用户-角色关联服务实现。支持全量替换角色分配，自动计算变更集，
 * 权限变化时通过 TokenManager 主动清除用户登录态。
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRolePO> implements UserRoleService {
    private final TokenManager tokenManager;

    /**
     * 保存用户角色分配（全量替换）。流程：查询现有角色 → 计算新增/删除的变更集 →
     * 批量保存新增、删除废弃 → 角色变更时清除该用户的登录态。
     *
     * @param userId 用户 ID
     * @param roleIds 待分配的角色 ID 列表
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return;
        }

        List<Long> userRoleIds = this.list(new LambdaQueryWrapper<UserRolePO>()
                        .select(UserRolePO::getRoleId)
                        .eq(UserRolePO::getUserId, userId))
                .parallelStream()
                .map(UserRolePO::getRoleId)
                .toList();

        if (CollectionUtil.isEmpty(roleIds)) {
            this.remove(new LambdaQueryWrapper<UserRolePO>()
                    .eq(UserRolePO::getUserId, userId));
            if (CollectionUtil.isNotEmpty(userRoleIds)) {
                tokenManager.invalidateUserSessions(userId);
            }
            return;
        }

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
            tokenManager.invalidateUserSessions(userId);
        }
    }

    /**
     * 为角色分配用户（全量替换）。流程：查询现有用户 → 计算变更集 →
     * 清空该角色已有用户关联 → 重新写入用户关联 → 清除受影响用户的登录态。
     *
     * @param roleId 角色 ID
     * @param userIds 待分配的用户 ID 列表
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", allEntries = true)
    public void assignUsersToRole(Long roleId, List<Long> userIds) {
        if (roleId == null) {
            return;
        }

        Set<Long> oldUserIds = new HashSet<>(this.list(new LambdaQueryWrapper<UserRolePO>()
                        .select(UserRolePO::getUserId)
                        .eq(UserRolePO::getRoleId, roleId))
                .stream()
                .map(UserRolePO::getUserId)
                .toList());
        Set<Long> newUserIds = CollectionUtil.isEmpty(userIds)
                ? new HashSet<>()
                : new HashSet<>(userIds);
        Set<Long> changedUserIds = new HashSet<>(oldUserIds);
        changedUserIds.addAll(newUserIds);
        Set<Long> unchangedUserIds = new HashSet<>(oldUserIds);
        unchangedUserIds.retainAll(newUserIds);
        changedUserIds.removeAll(unchangedUserIds);

        // 先清空该角色已有的用户关联
        this.remove(new LambdaQueryWrapper<UserRolePO>()
                .eq(UserRolePO::getRoleId, roleId)
        );

        if (CollectionUtil.isEmpty(userIds)) {
            changedUserIds.forEach(tokenManager::invalidateUserSessions);
            return;
        }

        // 重新写入用户与角色映射
        this.saveBatch(userIds.stream()
                .distinct()
                .map(userId -> new UserRolePO(userId, roleId))
                .toList());
        changedUserIds.forEach(tokenManager::invalidateUserSessions);
    }

    /**
     * 根据用户 ID 列表删除对应的用户-角色关联记录。通常在删除用户时调用。
     *
     * @param userIds 待删除关联的用户 ID 列表
     */
    @Override
    @Transactional
    public void removeUserRolesByUserIds(List<Long> userIds) {
        if (CollectionUtil.isEmpty(userIds)) {
            return;
        }
        this.remove(new LambdaQueryWrapper<UserRolePO>()
                .in(UserRolePO::getUserId, userIds));
    }

    /**
     * 批量保存用户-角色关联记录（按指定批次大小提交）。
     *
     * @param userRoles 用户角色关联列表
     * @param batchSize 每批提交的条数
     */
    @Override
    @Transactional
    public void saveBatchUserRoles(List<UserRolePO> userRoles, int batchSize) {
        if (CollectionUtil.isEmpty(userRoles)) {
            return;
        }
        this.saveBatch(userRoles, batchSize);
    }

    /**
     * 判断指定角色是否已分配了用户。统计该角色关联的用户数。
     *
     * @param roleId 角色 ID
     * @return true 表示已有用户绑定，false 表示未绑定
     */
    @Override
    public boolean hasAssignedUsers(Long roleId) {
        int count = this.baseMapper.countUsersForRole(roleId);
        return count > 0;
    }
}
