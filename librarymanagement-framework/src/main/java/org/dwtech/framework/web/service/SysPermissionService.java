package org.dwtech.framework.web.service;

import org.dwtech.common.core.entity.SysUser;
import org.dwtech.system.service.SysRoleService;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SysPermissionService {
    private final SysRoleService sysRoleService;

    public SysPermissionService(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    public Set<String> getMenuPermissions(SysUser user) {
        Set<String> perms = new HashSet<>();
        if (user.isAdmin()) {
            perms.add("*:*:*");
        }
        return perms;
    }

    /**
     * 获取角色数据权限
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new HashSet<>();
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            roles.add("admin");
        } else {
            roles.addAll(sysRoleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }
}
