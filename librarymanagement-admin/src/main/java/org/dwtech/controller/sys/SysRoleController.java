package org.dwtech.controller.sys;

import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.SysRole;
import org.dwtech.system.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/role")
public class SysRoleController {
    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @GetMapping("/list")
    @PreAuthorize(value = "@yz.hasPermit('system:role:list')")
    public AjaxResult getRole(SysRole role) {
        return AjaxResult.success(sysRoleService.selectRoleList(role));
    }
}
