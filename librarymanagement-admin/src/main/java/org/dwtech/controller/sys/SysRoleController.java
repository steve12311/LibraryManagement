package org.dwtech.controller.sys;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.SysRoleDto;
import org.dwtech.common.core.entity.vo.SysRoleVo;
import org.dwtech.common.valid.AddGroup;
import org.dwtech.common.valid.EditGroup;
import org.dwtech.system.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/system/role")
public class SysRoleController {
    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @GetMapping("/list")
    @PreAuthorize(value = "@yz.hasPermit('system:role:list')")
    public AjaxResult getRole(SysRoleDto role) {
        IPage<SysRoleDto> sysRoleDtoIPage = sysRoleService.selectRoleList(role);
        Page<SysRoleVo> sysRoleVoPage = new Page<>();
        BeanUtil.copyProperties(sysRoleDtoIPage, sysRoleVoPage);

        List<SysRoleVo> sysRoleVoList = new ArrayList<>();
        sysRoleDtoIPage.getRecords().forEach(sysRoleDto -> {
            SysRoleVo sysRoleVo = new SysRoleVo();
            BeanUtil.copyProperties(sysRoleDto, sysRoleVo);
            sysRoleVoList.add(sysRoleVo);
        });
        sysRoleVoPage.setRecords(sysRoleVoList);
        return AjaxResult.success(sysRoleVoPage);
    }

    @PostMapping
    @PreAuthorize(value = "@yz.hasPermit('system:role:add')")
    public AjaxResult addRole(@Validated(AddGroup.class) @RequestBody SysRoleDto role) {
        return AjaxResult.success(sysRoleService.insertRole(role));
    }

    @PutMapping
    @PreAuthorize(value = "@yz.hasPermit('system:role:edit')")
    public AjaxResult editRole(@Validated(EditGroup.class) @RequestBody SysRoleDto role) {
        return AjaxResult.success(sysRoleService.updateRole(role));
    }

    @DeleteMapping("/{roleIds}")
    @PreAuthorize(value = "@yz.hasPermit('system:role:del')")
    public AjaxResult delRole(@PathVariable(value = "roleIds") Long[] roleIds) {
        return AjaxResult.success(sysRoleService.deleteRole(roleIds));
    }
}
