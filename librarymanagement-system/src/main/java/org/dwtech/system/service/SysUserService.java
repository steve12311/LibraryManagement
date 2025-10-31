package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.SysUserDto;

public interface SysUserService {
    SysUserDto selectUserByUserName(String username);

    Integer updateUser(SysUserDto sysUser);

    Integer updateLoginInfo(Long userId, String loginIp);

    IPage<SysUserDto> selectUserList(SysUserDto sysUser);

    Integer insertUser(SysUserDto sysUserDto);

    Integer deleteUser(Long[] id);

    boolean checkUserNameUnique(SysUserDto sysUserDto);

    boolean checkUserPhonenumberUnique(SysUserDto sysUserDto);

    boolean checkUserEmailUnique(SysUserDto sysUserDto);

    boolean hasUserById(SysUserDto sysUserDto);

    boolean checkIdsExist(Long[] ids);
}
