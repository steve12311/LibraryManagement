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
 * 角色管理服务，提供角色分页查询、下拉选项、增删改、状态管理、菜单/用户分配功能。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface RoleService {
    /**
     * 根据角色编码集合获取最大的数据权限范围。
     *
     * @param roles 角色编码集合
     * @return 最大的数据权限数值
     */
    Integer getMaximumDataScope(Set<String> roles);

    /**
     * 分页查询角色列表。
     *
     * @param queryParams 分页查询参数（页码、每页条数、筛选条件）
     * @return 角色分页结果
     */
    Page<RolePageVO> getRolePage(RolePageQuery queryParams);

    /**
     * 查询所有角色的下拉选项列表，供前端角色选择器使用。
     *
     * @return 角色选项列表
     */
    List<Option<Long>> listRoleOptions();

    /**
     * 新增角色。
     *
     * @param roleForm 角色表单（角色名称、编码、权限范围等）
     * @return true 表示新增成功，false 表示失败
     */
    boolean saveRole(@Valid RoleForm roleForm);

    /**
     * 根据 ID 查询角色表单数据（用于编辑回显）。
     *
     * @param roleId 角色 ID
     * @return 角色表单
     */
    RoleForm getRoleForm(Long roleId);

    /**
     * 批量删除角色（逗号分隔的 ID 字符串）。
     *
     * @param ids 逗号分隔的角色主键 ID
     */
    void deleteRoles(String ids);

    /**
     * 更新角色状态（启用/禁用）。
     *
     * @param roleId 角色 ID
     * @param status 目标状态值
     * @return true 表示更新成功，false 表示失败
     */
    boolean updateRoleStatus(Long roleId, Integer status);

    /**
     * 查询指定角色已分配的菜单 ID 列表。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 为角色分配菜单权限。
     *
     * @param roleId 角色 ID
     * @param menuIds 待分配的菜单 ID 列表
     */
    void assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 为角色分配用户。
     *
     * @param roleId 角色 ID
     * @param userIds 待分配的用户 ID 列表
     */
    void assignUsersToRole(Long roleId, List<Long> userIds);
}
