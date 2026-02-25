package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.system.model.entity.RoleMenuPO;

import java.util.List;
import java.util.Set;
/**
 * RoleMenuService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface RoleMenuService extends IService<RoleMenuPO> {
    /**
     * 用途：查询 menu ids by role id 列表。
     * 
     * 获取角色拥有的菜单ID集合
     *
     * @param roleId 角色ID
     * @return 菜单ID集合
     */
    List<Long> listMenuIdsByRoleId(Long roleId);


    /**
     * 用途：刷新 role perms cache。
     * 
     * 刷新权限缓存(所有角色)
     * 
     * 入参：无。
     * 返回：无。
     */
    void refreshRolePermsCache();

    /**
     * 用途：刷新 role perms cache。
     * 
     * 刷新权限缓存(指定角色)
     *
     * @param roleCode 角色编码
     * 返回：无。
     */
    void refreshRolePermsCache(String roleCode);

    /**
     * 用途：刷新 role perms cache。
     * 
     * 刷新权限缓存(修改角色编码时调用)
     *
     * @param oldRoleCode 旧角色编码
     * @param newRoleCode 新角色编码
     * 返回：无。
     */
    void refreshRolePermsCache(String oldRoleCode, String newRoleCode);

    /**
     * 用途：获取 role perms by role codes 信息。
     * 
     * 获取角色权限集合
     *
     * @param roles 角色编码集合
     * @return 权限集合
     */
    Set<String> getRolePermsByRoleCodes(Set<String> roles);
}
