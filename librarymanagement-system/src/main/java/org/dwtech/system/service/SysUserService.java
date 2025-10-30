package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.SysUser;

public interface SysUserService {
    SysUser selectUserByUserName(String username);

    Integer updateUser(SysUser sysUser);

    Integer updateLoginInfo(Long userId, String loginIp);

    IPage<SysUser> selectUserList(SysUser sysUser);
}
