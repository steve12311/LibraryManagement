package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.RoleForm;
import org.dwtech.system.model.query.RolePageQuery;
import org.dwtech.system.model.vo.RolePageVO;

import java.util.List;
import java.util.Set;
/**
 * RoleService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface RoleService {
    /**
     * 用途：获取 maximum data scope 信息。
     * 
     * @param roles roles
     * @return 数值结果
     */
    Integer getMaximumDataScope(Set<String> roles);

    /**
     * 用途：获取 role page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    Page<RolePageVO> getRolePage(RolePageQuery queryParams);

    /**
     * 用途：查询 role options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<Option<Long>> listRoleOptions();

    /**
     * 用途：保存 role。
     * 
     * @param roleForm role form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean saveRole(@Valid RoleForm roleForm);

    /**
     * 用途：获取 role form 信息。
     * 
     * @param roleId role ID
     * @return 返回结果
     */
    RoleForm getRoleForm(Long roleId);

    /**
     * 用途：删除 roles。
     * 
     * @param ids 主键 ID 列表
     * 返回：无。
     */
    void deleteRoles(String ids);

    /**
     * 用途：更新 role status。
     * 
     * @param roleId role ID
     * @param status status
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean updateRoleStatus(Long roleId, Integer status);

    /**
     * 用途：获取 role menu ids 信息。
     * 
     * @param roleId role ID
     * @return 结果列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 用途：分配 menus to role。
     * 
     * @param roleId role ID
     * @param menuIds menu ID 列表
     * 返回：无。
     */
    void assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 用途：分配 users to role。
     * 
     * @param roleId role ID
     * @param userIds user ID 列表
     * 返回：无。
     */
    void assignUsersToRole(Long roleId, List<Long> userIds);
}
