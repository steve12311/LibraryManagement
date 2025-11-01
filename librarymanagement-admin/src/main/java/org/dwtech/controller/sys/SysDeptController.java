package org.dwtech.controller.sys;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        IPage<SysDeptDto> page = sysDeptService.buildDeptTree(sysDeptService.selectDeptList(sysDept));
        Page<SysDeptVo> sysDeptVoPage = new Page<>();
        BeanUtil.copyProperties(page, sysDeptVoPage);

        List<SysDeptVo> sysDeptVoList = page.getRecords().stream()
                .map(this::convertDeptDtoToVo)
                .toList();

        sysDeptVoPage.setRecords(sysDeptVoList);
        return AjaxResult.success(sysDeptVoPage);
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
     * 递归将单个部门DTO转换为VO
     */
    private SysDeptVo convertDeptDtoToVo(SysDeptDto deptDto) {
        SysDeptVo deptVo = new SysDeptVo();
        BeanUtil.copyProperties(deptDto, deptVo);

        // 递归处理子部门
        List<SysDeptDto> children = deptDto.getChildren();
        if (children != null && !children.isEmpty()) {
            List<SysDeptVo> childVos = children.stream()
                    .map(this::convertDeptDtoToVo)
                    .toList();
            deptVo.setChildren(childVos);
        } else {
            // 如果children为null或空，设置为空列表
            deptVo.setChildren(null);
        }

        return deptVo;
    }
}
