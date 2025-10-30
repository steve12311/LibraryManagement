package org.dwtech.controller.sys;

import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.SysUser;
import org.dwtech.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("list")
    @PreAuthorize(value = "@yz.hasPermit('system:user:list')")
    public AjaxResult getUser(SysUser sysUser) {
        return AjaxResult.success(sysUserService.selectUserList(sysUser));
    }
}
