package org.dwtech.system.service;

import org.dwtech.system.model.entity.UserRolePO;

import java.util.List;
/**
 * 用户-角色关联服务，负责用户角色分配、批量保存及关联关系删除。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface UserRoleService {
    /**
     * 保存用户角色分配。自动计算新增和删除的角色变更集，并在权限变化时主动清除用户登录态。
     *
     * @param userId 用户 ID
     * @param roleIds 待分配的角色 ID 列表（全量替换）
     */
    void saveUserRoles(Long userId, List<Long> roleIds);

    /**
     * 为角色分配用户。先清空该角色已有的用户关联，再重新写入，并清除受影响用户的登录态。
     *
     * @param roleId 角色 ID
     * @param userIds 待分配的用户 ID 列表（全量替换）
     */
    void assignUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 根据用户 ID 列表删除对应的用户-角色关联记录。
     *
     * @param userIds 用户 ID 列表
     */
    void removeUserRolesByUserIds(List<Long> userIds);

    /**
     * 批量保存用户-角色关联记录。
     *
     * @param userRoles 用户角色关联列表
     * @param batchSize 每批提交的数量
     */
    void saveBatchUserRoles(List<UserRolePO> userRoles, int batchSize);

    /**
     * 判断指定角色是否已分配了用户。
     *
     * @param roleId 角色 ID
     * @return true 表示已有用户绑定，false 表示未绑定
     */
    boolean hasAssignedUsers(Long roleId);
}
