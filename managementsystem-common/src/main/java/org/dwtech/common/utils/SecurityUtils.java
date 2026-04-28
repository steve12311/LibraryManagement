package org.dwtech.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.constant.SystemConstants;
import org.dwtech.common.core.entity.SysUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 工具类
 *
 * @author steve12311
 * @since 2025-11-18
 */
public class SecurityUtils {

    /**
     * 获取当前登录人信息。
     *
     * @return 当前登录用户详情（Optional）
     */
    public static Optional<SysUserDetails> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof SysUserDetails) {
                return Optional.of((SysUserDetails) principal);
            }
        }
        return Optional.empty();
    }


    /**
     * 获取用户 ID。
     *
     * @return 当前用户 ID
     */
    public static Long getUserId() {
        return getUser().map(SysUserDetails::getUserId).orElse(null);
    }


    /**
     * 获取用户账号。
     *
     * @return 当前用户账号
     */
    public static String getUsername() {
        return getUser().map(SysUserDetails::getUsername).orElse(null);
    }


    /**
     * 获取部门 ID。
     *
     * @return 当前用户部门 ID
     */
    public static Long getDeptId() {
        return getUser().map(SysUserDetails::getDeptId).orElse(null);
    }

    /**
     * 获取数据权限范围。
     *
     * @return 当前用户数据权限范围值
     */
    public static Integer getDataScope() {
        return getUser().map(SysUserDetails::getDataScope).orElse(null);
    }


    /**
     * 获取当前登录人的角色集合。
     *
     * @return 角色集合
     */
    public static Set<String> getRoles() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getAuthorities)
                .filter(CollectionUtil::isNotEmpty)
                .stream()
                .flatMap(Collection::stream)
                .map(GrantedAuthority::getAuthority)
                // 筛选角色,authorities 中的角色都是以 ROLE_ 开头
                .filter(authority -> authority.startsWith(SecurityConstants.ROLE_PREFIX))
                .map(authority -> StrUtil.removePrefix(authority, SecurityConstants.ROLE_PREFIX))
                .collect(Collectors.toSet());
    }

    /**
     * 判断当前登录人是否超级管理员（超级管理员忽视任何权限判断）。
     *
     * @return 是超级管理员返回 true
     */
    public static boolean isRoot() {
        Set<String> roles = getRoles();
        return roles.contains(SystemConstants.ROOT_ROLE_CODE);
    }

    /**
     * 从当前请求中提取 Authorization Token。
     *
     * @return Token 字符串
     */
    public static String getTokenFromRequest() {
        ServletRequestAttributes servletRequestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        if(Objects.isNull(servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }


}
