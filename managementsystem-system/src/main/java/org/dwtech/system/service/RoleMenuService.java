package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.system.model.entity.RoleMenuPO;

import java.util.List;
import java.util.Set;
/**
 * 角色-菜单关联服务，负责角色权限查询、菜单 ID 查询及权限缓存刷新。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface RoleMenuService extends IService<RoleMenuPO> {
    /**
     * 查询指定角色拥有的菜单 ID 集合。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> listMenuIdsByRoleId(Long roleId);


    /**
     * 刷新所有角色的权限缓存。从数据库重新加载全部角色权限并写入 Redis。
     */
    void refreshRolePermsCache();

    /**
     * 刷新指定角色的权限缓存。
     *
     * @param roleCode 角色编码
     */
    void refreshRolePermsCache(String roleCode);

    /**
     * 刷新因角色编码变更导致的权限缓存（清理旧编码、写入新编码）。
     *
     * @param oldRoleCode 旧角色编码
     * @param newRoleCode 新角色编码
     */
    void refreshRolePermsCache(String oldRoleCode, String newRoleCode);

    /**
     * 根据角色编码集合查询对应的权限标识集合。
     *
     * @param roles 角色编码集合
     * @return 权限标识集合
     */
    Set<String> getRolePermsByRoleCodes(Set<String> roles);
}
