package org.dwtech.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.SysRoleDto;
import org.dwtech.common.core.entity.po.SysRolePo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.SysRoleMapper;
import org.dwtech.system.service.SysRoleService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SysRoleServiceImpl implements SysRoleService {
    private final SysRoleMapper sysRoleMapper;

    public SysRoleServiceImpl(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRolePo> perms = sysRoleMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRolePo perm : perms) {
            if (perm != null) {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    @Override
    public IPage<SysRoleDto> selectRoleList(SysRoleDto role) {
        SysRolePo rolePo = new SysRolePo();
        BeanUtil.copyProperties(role, rolePo);

        Page<SysRoleDto> page = new Page<>();
        IPage<SysRolePo> sysUserPoIPage = sysRoleMapper.selectRoleList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageStart()), rolePo, PageUtils.getCondition());
        BeanUtil.copyProperties(sysUserPoIPage, page);

        List<SysRoleDto> sysRoleDtoList = new ArrayList<>();
        sysUserPoIPage.getRecords().forEach(sysUserPo -> {
            SysRoleDto sysRoleDto = new SysRoleDto();
            BeanUtil.copyProperties(sysUserPo, sysRoleDto);
            sysRoleDtoList.add(sysRoleDto);
        });
        page.setRecords(sysRoleDtoList);
        return page;
    }

    @Override
    public Integer insertRole(SysRoleDto role) {
        SysRolePo rolePo = new SysRolePo();
        BeanUtil.copyProperties(role, rolePo);
        rolePo.setCreateBy(SecurityUtils.getUsername());
        return sysRoleMapper.insertRole(rolePo);
    }

    @Override
    public Integer updateRole(SysRoleDto role) {
        SysRolePo rolePo = new SysRolePo();
        BeanUtil.copyProperties(role, rolePo);
        rolePo.setUpdateBy(SecurityUtils.getUsername());
        return sysRoleMapper.updateRole(rolePo);
    }

    @Override
    public Integer deleteRole(Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return 0;
        } else if (ids.length == 1) {
            return sysRoleMapper.deleteRoleById(ids[0]);
        }
        return 0;
    }
}
