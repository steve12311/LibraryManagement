package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/page")
    public PageResult<RolePageVO> getRolePage(
            RolePageQuery queryParams
    ) {
        Page<RolePageVO> result = roleService.getRolePage(queryParams);
        return PageResult.success(result);
    }

    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoleOptions() {
        List<Option<Long>> list = roleService.listRoleOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:role:add')")
    @RepeatSubmit
    public Result<?> addRole(@Valid @RequestBody RoleForm roleForm) {
        boolean result = roleService.saveRole(roleForm);
        return Result.judge(result);
    }

    @Operation(summary = "获取角色表单数据")
    @GetMapping("/{roleId}/form")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<RoleForm> getRoleForm(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId
    ) {
        RoleForm roleForm = roleService.getRoleForm(roleId);
        return Result.success(roleForm);
    }

    @Operation(summary = "修改角色")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<?> updateRole(@Valid @RequestBody RoleForm roleForm) {
        boolean result = roleService.saveRole(roleForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:role:delete')")
    public Result<Void> deleteRoles(
            @Parameter(description = "删除角色，多个以英文逗号(,)拼接") @PathVariable("ids") String ids
    ) {
        roleService.deleteRoles(ids);
        return Result.success();
    }

    @Operation(summary = "修改角色状态")
    @PutMapping(value = "/{roleId}/status")
    @PreAuthorize("@ss.hasPerm('sys:role:edit')")
    public Result<?> updateRoleStatus(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId,
            @Parameter(description = "状态(1:启用;0:禁用)") @RequestParam("status") Integer status
    ) {
        boolean result = roleService.updateRoleStatus(roleId, status);
        return Result.judge(result);
    }

    @Operation(summary = "获取角色的菜单ID集合")
    @GetMapping("/{roleId}/menuIds")
    public Result<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId
    ) {
        List<Long> menuIds = roleService.getRoleMenuIds(roleId);
        return Result.success(menuIds);
    }

    @Operation(summary = "角色分配菜单权限")
    @PutMapping("/{roleId}/menus")
    public Result<Void> assignMenusToRole(
            @PathVariable("roleId") Long roleId,
            @RequestBody List<Long> menuIds
    ) {
        roleService.assignMenusToRole(roleId, menuIds);
        return Result.success();
    }

    @PutMapping("/{roleId}/users")
    public Result<Void> assignUsersToRole(@PathVariable("roleId") Long roleId, @RequestParam("userIds") List<Long> userIds) {
        roleService.assignUserToRole(roleId, userIds);
        return Result.success();
    }
}
