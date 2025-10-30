package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.SysUser;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.system.mapper.SysUserMapper;
import org.dwtech.system.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements SysUserService {
    private final SysUserMapper sysUserMapper;

    public SysUserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public SysUser selectUserByUserName(String username) {
        return sysUserMapper.selectUserByUserName(username);
    }

    @Override
    public Integer updateUser(SysUser sysUser) {
        return sysUserMapper.updateUser(sysUser);
    }

    @Override
    public Integer updateLoginInfo(Long userId, String loginIp) {
        return sysUserMapper.updateLoginInfo(userId, loginIp);
    }

    @Override
    public IPage<SysUser> selectUserList(SysUser sysUser) {
        return sysUserMapper.selectUserList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), sysUser, PageUtils.getCondition());
    }
}
