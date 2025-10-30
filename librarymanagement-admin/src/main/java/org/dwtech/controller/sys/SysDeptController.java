package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.SysDept;
import org.dwtech.system.service.SysDeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/dept")
public class SysDeptController {
    private final SysDeptService sysDeptService;

    public SysDeptController(SysDeptService sysDeptService) {
        this.sysDeptService = sysDeptService;
    }

    @GetMapping("/list")
    @PreAuthorize(value = "@yz.hasPermit('system:dept:list')")
    public AjaxResult getDeptList(SysDept sysDept) {
        IPage<SysDept> page = sysDeptService.selectDeptList(sysDept);
        return AjaxResult.success(sysDeptService.buildDeptTree(page));
    }
}
