package org.dwtech.controller;

import cn.hutool.core.bean.BeanUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.Constants;
import org.dwtech.common.core.entity.*;
import org.dwtech.common.core.entity.dto.SysUserDto;
import org.dwtech.common.core.entity.vo.UserInfoVo;
import org.dwtech.framework.web.service.SysLoginService;
import org.dwtech.framework.web.service.SysPermissionService;
import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.utils.DateUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.framework.web.service.TokenService;
import org.dwtech.system.service.SysConfigService;
import org.dwtech.system.service.SysDeptService;
import org.dwtech.system.service.SysMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
public class LoginController extends BaseController {
    private final SysLoginService sysLoginService;
    private final SysPermissionService sysPermissionService;
    private final TokenService tokenService;
    private final SysConfigService sysConfigService;
    private final SysMenuService sysMenuService;
    private final SysDeptService sysDeptService;

    public LoginController(SysLoginService sysLoginService, SysPermissionService sysPermissionService, TokenService tokenService, SysConfigService sysConfigService, SysMenuService sysMenuService, SysDeptService sysDeptService) {
        this.sysLoginService = sysLoginService;
        this.sysPermissionService = sysPermissionService;
        this.tokenService = tokenService;
        this.sysConfigService = sysConfigService;
        this.sysMenuService = sysMenuService;
        this.sysDeptService = sysDeptService;
    }

    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginBody loginBody) {
        log.info("登录表单: {}", loginBody);
        AjaxResult ajaxResult = AjaxResult.success();
        String token = sysLoginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(), loginBody.getUuid());
        ajaxResult.put(Constants.TOKEN, token);
        return ajaxResult;
    }

    @GetMapping("/getinfo")
    public AjaxResult getInfo() {
        UserInfoVo userInfoVo = new UserInfoVo();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUserDto user = loginUser.getUser();
        BeanUtil.copyProperties(user, userInfoVo);
        userInfoVo.setDeptName(sysDeptService.selectDeptById(user.getDeptId()).getDeptName());
        // 角色集合
        Set<String> roles = sysPermissionService.getRolePermission(user);
        userInfoVo.setRoles(roles);
        // 权限集合
        Set<String> permissions = sysPermissionService.getMenuPermissions(user);
        // 权限有变化，需要刷新
        if (!loginUser.getPermissions().equals(permissions)) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", userInfoVo);
        ajax.put("permissions", permissions);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    @GetMapping("/routers")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = sysMenuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(sysMenuService.buildMenus(menus));
    }

    private boolean initPasswordIsModify(Date pwdUpdateDate) {
        int flag = Integer.parseInt(sysConfigService.selectConfigByKey("sys.account.initPasswordModify"));
        return flag == 1 && pwdUpdateDate == null;
    }

    private boolean passwordIsExpiration(Date pwdUpdateDate) {
        int passwordValidateDays = Integer.parseInt(sysConfigService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays > 0) {
            if (pwdUpdateDate == null) {
                return true;
            }
            return DateUtils.differentDaysByMillisecond(pwdUpdateDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
