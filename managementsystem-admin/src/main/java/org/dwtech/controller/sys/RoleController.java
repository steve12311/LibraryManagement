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
     * 用途：获取 role page 信息。
     * 
     * @param queryParams query params
     * @return 返回结果
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
     * 用途：查询 role options 列表。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public Result<List<Option<Long>>> listRoleOptions() {
        List<Option<Long>> list = roleService.listRoleOptions();
        return Result.success(list);
    }

    /**
     * 用途：新增 role。
     * 
     * @param roleForm role form
     * @return 返回结果
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
     * 用途：获取 role form 信息。
     * 
     * @param roleId role ID
     * @return 返回结果
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
     * 用途：更新 role。
     * 
     * @param roleId role ID
     * @param roleForm role form
     * @return 返回结果
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
     * 用途：删除 roles。
     * 
     * @param ids 主键 ID 列表
     * @return 返回结果
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
     * 用途：更新 role status。
     * 
     * @param roleId role ID
     * @param status status
     * @return 返回结果
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
     * 用途：获取 role menu ids 信息。
     * 
     * @param roleId role ID
     * @return 返回结果
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
     * 用途：分配 menus to role。
     * 
     * @param roleId role ID
     * @param menuIds menu ID 列表
     * @return 返回结果
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
     * 用途：分配 users to role。
     * 
     * @param roleId role ID
     * @param userIds user ID 列表
     * @return 返回结果
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
