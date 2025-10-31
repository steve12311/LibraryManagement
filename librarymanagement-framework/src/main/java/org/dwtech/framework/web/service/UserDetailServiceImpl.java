package org.dwtech.framework.web.service;

import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.LoginUser;
import org.dwtech.common.core.entity.dto.SysUserDto;
import org.dwtech.common.enums.UserStatus;
import org.dwtech.common.exception.ServiceException;
import org.dwtech.system.service.SysUserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private final SysPasswordService sysPasswordService;

    private final SysUserService sysUserService;

    private final SysPermissionService sysPermissionService;


    public UserDetailServiceImpl(SysPasswordService sysPasswordService, SysUserService sysUserService, SysPermissionService sysPermissionService) {
        this.sysPasswordService = sysPasswordService;
        this.sysUserService = sysUserService;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        SysUserDto user = sysUserService.selectUserByUserName(username);
        if (user == null) {
            log.info("登录用户：{} 不存在", username);
            throw new ServiceException("用户不存在");
        } else if (user.getStatus().equals(UserStatus.DISABLE.getCode())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new ServiceException("用户被禁用");
        } else if (user.getStatus().equals(UserStatus.DELETED.getCode())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException("用户被删除");
        }
        sysPasswordService.validate(user);
        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUserDto user) {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, sysPermissionService.getMenuPermissions(user));
    }
}
