package org.dwtech.controller.sys;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.SysDeptDto;
import org.dwtech.common.core.entity.vo.SysDeptVo;
import org.dwtech.common.valid.SysAddDeptGroup;
import org.dwtech.common.valid.SysEditDeptGroup;
import org.dwtech.system.service.SysDeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {
    private final SysDeptService sysDeptService;

    public SysDeptController(SysDeptService sysDeptService) {
        this.sysDeptService = sysDeptService;
    }

    @GetMapping("/list")
    @PreAuthorize(value = "@yz.hasPermit('system:dept:list')")
    public AjaxResult getDeptList(SysDeptDto sysDept) {
        List<SysDeptDto> sysDeptDtoTree = sysDeptService.buildDeptTree(sysDeptService.selectDeptList(sysDept));
        List<SysDeptVo> sysDeptTree = new ArrayList<>();
        sysDeptDtoTree.forEach(item -> sysDeptTree.add(convertToVo(item)));
        return AjaxResult.success(sysDeptTree);
    }

    @PostMapping
    @PreAuthorize(value = "@yz.hasPermit('system:dept:add')")
    public AjaxResult addDept(@Validated(SysAddDeptGroup.class) @RequestBody SysDeptDto sysDept) {
        return AjaxResult.success(sysDeptService.insertDept(sysDept));
    }

    @PutMapping
    @PreAuthorize(value = "@yz.hasPermit('system:dept:edit')")
    public AjaxResult editDept(@Validated(SysEditDeptGroup.class) @RequestBody SysDeptDto sysDept) {
        return AjaxResult.success(sysDeptService.updateDept(sysDept));
    }

    @DeleteMapping("/{deptIds}")
    @PreAuthorize(value = "@yz.hasPermit('system:dept:del')")
    public AjaxResult deleteDept(@PathVariable(value = "deptIds") Long[] deptIds) {
        return AjaxResult.success(sysDeptService.deleteDept(deptIds));
    }

    /**
     * 部门DTO转换为VO
     */
    private SysDeptVo convertToVo(SysDeptDto dto) {
        return BeanUtil.copyProperties(dto, SysDeptVo.class);
    }
}
