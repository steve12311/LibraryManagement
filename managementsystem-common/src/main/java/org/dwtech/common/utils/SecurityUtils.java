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
     * 用途：获取 user 信息。
     * 
     * 获取当前登录人信息
     *
     * @return Optional<SysUserDetails>
     * 入参：无。
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
     * 用途：获取 user id 信息。
     * 
     * 获取用户ID
     *
     * @return Long
     * 入参：无。
     */
    public static Long getUserId() {
        return getUser().map(SysUserDetails::getUserId).orElse(null);
    }


    /**
     * 用途：获取 username 信息。
     * 
     * 获取用户账号
     *
     * @return String 用户账号
     * 入参：无。
     */
    public static String getUsername() {
        return getUser().map(SysUserDetails::getUsername).orElse(null);
    }


    /**
     * 用途：获取 dept id 信息。
     * 
     * 获取部门ID
     *
     * @return Long
     * 入参：无。
     */
    public static Long getDeptId() {
        return getUser().map(SysUserDetails::getDeptId).orElse(null);
    }

    /**
     * 用途：获取 data scope 信息。
     * 
     * 获取数据权限范围
     *
     * @return Integer
     * 入参：无。
     */
    public static Integer getDataScope() {
        return getUser().map(SysUserDetails::getDataScope).orElse(null);
    }


    /**
     * 用途：获取 roles 信息。
     * 
     * 获取角色集合
     *
     * @return 角色集合
     * 入参：无。
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
     * 用途：判断 root 状态。
     * 
     * 是否超级管理员
     * <p>
     * 超级管理员忽视任何权限判断
     * 
     * 入参：无。
     * @return 操作结果，true 表示成功，false 表示失败
     */
    public static boolean isRoot() {
        Set<String> roles = getRoles();
        return roles.contains(SystemConstants.ROOT_ROLE_CODE);
    }

    /**
     * 用途：获取 token from request 信息。
     * 
     * 获取请求中的 Token
     *
     * @return Token 字符串
     * 入参：无。
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
