package org.dwtech.controller.sys;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.SysDeptDto;
import org.dwtech.common.core.entity.dto.SysUserDto;
import org.dwtech.common.core.entity.vo.SysUserVo;
import org.dwtech.common.exception.NotValidException;
import org.dwtech.common.valid.AddGroup;
import org.dwtech.common.valid.EditGroup;
import org.dwtech.system.service.SysDeptService;
import org.dwtech.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    private final SysUserService sysUserService;
    private final SysDeptService sysDeptService;

    public SysUserController(SysUserService sysUserService, SysDeptService sysDeptService) {
        this.sysUserService = sysUserService;
        this.sysDeptService = sysDeptService;
    }

    @GetMapping("list")
    @PreAuthorize(value = "@yz.hasPermit('system:user:list')")
    public AjaxResult getUser(SysUserDto sysUserDto) {
        IPage<SysUserDto> page = sysUserService.selectUserList(sysUserDto);
        Page<SysUserVo> sysUserVoPage = new Page<>();
        BeanUtil.copyProperties(page, sysUserVoPage);

        // 提取所有部门ID
        Long[] deptIds = page.getRecords().stream()
                .map(SysUserDto::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(Long[]::new);

        List<SysDeptDto> sysDeptDtos = sysDeptService.selectDeptByIds(deptIds);
        // 将部门列表转换为Map，key为部门ID，value为部门名称
        Map<Long, String> deptMap = sysDeptDtos.stream()
                .collect(Collectors.toMap(
                        SysDeptDto::getDeptId,
                        SysDeptDto::getDeptName
                ));

        List<SysUserVo> sysUserListVoList = new ArrayList<>();
        page.getRecords().forEach(sysUser -> {
            SysUserVo sysUserVo = new SysUserVo();
            BeanUtil.copyProperties(sysUser, sysUserVo);
            sysUserVo.setDeptName(deptMap.get(sysUser.getDeptId()));
            sysUserListVoList.add(sysUserVo);
        });
        sysUserVoPage.setRecords(sysUserListVoList);
        return AjaxResult.success(sysUserVoPage);
    }

    @PostMapping
    @PreAuthorize(value = "@yz.hasPermit('system:user:add')")
    public AjaxResult addUser(@Validated(AddGroup.class) @RequestBody SysUserDto sysUserDto) {
        if (sysUserService.checkUserNameUnique(sysUserDto)) {
            throw new NotValidException(String.format("用户名：%s 已存在", sysUserDto.getUserName()));
        }
        if (sysUserService.checkUserPhonenumberUnique(sysUserDto)) {
            throw new NotValidException(String.format("手机号：%s 已存在", sysUserDto.getPhonenumber()));
        }
        if (sysUserService.checkUserEmailUnique(sysUserDto)) {
            throw new NotValidException(String.format("邮箱：%s 已存在", sysUserDto.getEmail()));
        }
        // 新增用户
        return AjaxResult.success(sysUserService.insertUser(sysUserDto));
    }

    @PutMapping
    @PreAuthorize(value = "@yz.hasPermit('system:user:edit')")
    public AjaxResult editUser(@Validated(EditGroup.class) @RequestBody SysUserDto sysUserDto) {
        if (!sysUserService.hasUserById(sysUserDto)) {
            throw new NotValidException(String.format("用户Id：%d 不存在", sysUserDto.getUserId()));
        }
        return AjaxResult.success(sysUserService.updateUser(sysUserDto));
    }

    @DeleteMapping("/{userIds}")
    @PreAuthorize(value = "@yz.hasPermit('system:user:del')")
    public AjaxResult deleteUser(@PathVariable(value = "userIds") Long[] userIds) {
        if (!sysUserService.checkIdsExist(userIds)) {
            throw new NotValidException("Ids中存在不存在的用户Id");
        }
        return AjaxResult.success(sysUserService.deleteUser(userIds));
    }
}
