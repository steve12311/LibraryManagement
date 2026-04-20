package org.dwtech.system.service;

import org.dwtech.system.model.entity.UserRolePO;

import java.util.List;
/**
 * UserRoleService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface UserRoleService {
    /**
     * 用途：保存 user roles。
     * 
     * 保存用户角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * 返回：无。
     */
    void saveUserRoles(Long userId, List<Long> roleIds);

    /**
     * 用途：分配 users to role。
     * 
     * 为角色分配用户
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     * 返回：无。
     */
    void assignUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 用途：删除 users 对应的 role 关联。
     *
     * @param userIds 用户ID列表
     * 返回：无。
     */
    void removeUserRolesByUserIds(List<Long> userIds);

    /**
     * 用途：批量保存 user roles。
     *
     * @param userRoles 用户角色关联列表
     * @param batchSize 批量大小
     * 返回：无。
     */
    void saveBatchUserRoles(List<UserRolePO> userRoles, int batchSize);

    /**
     * 用途：判断是否存在 assigned users。
     * 
     * 判断角色是否存在绑定的用户
     *
     * @param roleId 角色ID
     * @return true：已分配 false：未分配
     */
    boolean hasAssignedUsers(Long roleId);
}
