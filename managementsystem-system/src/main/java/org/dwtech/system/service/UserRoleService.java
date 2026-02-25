package org.dwtech.system.service;

import java.util.List;

public interface UserRoleService {
    /**
     * 保存用户角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    void saveUserRoles(Long userId, List<Long> roleIds);

    /**
     * 为角色分配用户
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void assignUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 判断角色是否存在绑定的用户
     *
     * @param roleId 角色ID
     * @return true：已分配 false：未分配
     */
    boolean hasAssignedUsers(Long roleId);
}
