package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.RoleForm;
import org.dwtech.system.model.query.RolePageQuery;
import org.dwtech.system.model.vo.RolePageVO;
import org.dwtech.system.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * RoleController
 *
 * @author steve12311
 * @since 2025-11-18
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    /**
     * 分页查询角色列表。
     * 支持按角色名称、状态、创建时间等条件筛选。
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public PageResult<RolePageVO> getRolePage(
            RolePageQuery queryParams
    ) {
        Page<RolePageVO> result = roleService.getRolePage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 查询角色选项列表。
     * 返回角色的 ID 和名称键值对，用于前端下拉选择器。
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public Result<List<Option<Long>>> listRoleOptions() {
        List<Option<Long>> list = roleService.listRoleOptions();
        return Result.success(list);
    }

    /**
     * 新增角色。
     * 接收角色表单数据，校验角色编码唯一性后写入数据库并记录操作日志。
     */
    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:role:add')")
    @RepeatSubmit
    @OperLog(module = "角色管理", action = "新增角色", bizId = "#p0.code")
    public Result<?> addRole(@Valid @RequestBody RoleForm roleForm) {
        boolean result = roleService.saveRole(roleForm);
        return Result.judge(result);
    }

    /**
     * 根据 ID 获取角色表单数据，用于编辑时回显。
     *
     * @param roleId 角色 ID
     */
    @Operation(summary = "获取角色表单数据")
    @GetMapping("/{roleId}/form")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<RoleForm> getRoleForm(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId
    ) {
        RoleForm roleForm = roleService.getRoleForm(roleId);
        return Result.success(roleForm);
    }

    /**
     * 修改角色信息。
     * 根据路径 ID 和表单数据更新角色基本属性，校验角色编码唯一性并记录操作日志。
     */
    @Operation(summary = "修改角色")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    @RepeatSubmit
    @OperLog(module = "角色管理", action = "修改角色", bizId = "#p0")
    public Result<?> updateRole(
            @PathVariable("id") Long roleId,
            @Valid @RequestBody RoleForm roleForm
    ) {
        roleForm.setId(roleId);
        boolean result = roleService.saveRole(roleForm);
        return Result.judge(result);
    }

    /**
     * 批量删除角色。
     * 根据逗号分隔的角色 ID 字符串删除角色，若角色已分配用户则阻止删除。
     *
     * @param ids 角色 ID，多个以英文逗号拼接
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:role:delete')")
    @RepeatSubmit
    @OperLog(module = "角色管理", action = "删除角色", bizId = "#p0")
    public Result<Void> deleteRoles(
            @Parameter(description = "删除角色，多个以英文逗号(,)拼接") @PathVariable("ids") String ids
    ) {
        roleService.deleteRoles(ids);
        return Result.success();
    }

    /**
     * 修改角色启用/禁用状态。
     *
     * @param roleId 角色 ID
     * @param status 目标状态：1 启用，0 禁用
     */
    @Operation(summary = "修改角色状态")
    @PutMapping(value = "/{roleId}/status")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    @RepeatSubmit
    @OperLog(module = "角色管理", action = "修改角色状态", bizId = "#p0")
    public Result<?> updateRoleStatus(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId,
            @Parameter(description = "状态(1:启用;0:禁用)") @RequestParam("status") Integer status
    ) {
        boolean result = roleService.updateRoleStatus(roleId, status);
        return Result.judge(result);
    }

    /**
     * 获取指定角色已分配的菜单 ID 集合。
     * 用于前端角色菜单分配弹窗中回显已选菜单。
     *
     * @param roleId 角色 ID
     */
    @Operation(summary = "获取角色的菜单ID集合")
    @GetMapping("/{roleId}/menuIds")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId
    ) {
        List<Long> menuIds = roleService.getRoleMenuIds(roleId);
        return Result.success(menuIds);
    }

    /**
     * 为角色分配菜单权限。
     * 全量更新角色的菜单关联关系，先清除原有分配再插入新的菜单 ID 列表。
     *
     * @param roleId  角色 ID
     * @param menuIds 分配的菜单 ID 列表
     */
    @Operation(summary = "角色分配菜单权限")
    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    @RepeatSubmit
    @OperLog(module = "角色管理", action = "分配角色菜单", bizId = "#p0")
    public Result<Void> assignMenusToRole(
            @PathVariable("roleId") Long roleId,
            @RequestBody List<Long> menuIds
    ) {
        roleService.assignMenusToRole(roleId, menuIds);
        return Result.success();
    }

    /**
     * 为角色分配用户。
     * 全量更新角色的用户关联关系，先清除原有分配再插入新的用户 ID 列表。
     *
     * @param roleId  角色 ID
     * @param userIds 分配的用户 ID 列表
     */
    @Operation(summary = "角色分配用户")
    @PutMapping("/{roleId}/users")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    @RepeatSubmit
    @OperLog(module = "角色管理", action = "分配角色用户", bizId = "#p0")
    public Result<Void> assignUsersToRole(
            @PathVariable("roleId") Long roleId,
            @RequestParam("userIds") List<Long> userIds
    ) {
        roleService.assignUsersToRole(roleId, userIds);
        return Result.success();
    }
}
