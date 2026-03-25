package org.dwtech.common.core.entity;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dwtech.common.constant.SecurityConstants;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
/**
 * SysUserDetails
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
@NoArgsConstructor
public class SysUserDetails implements UserDetails {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 账号是否启用(true:启用 false:禁用)
     */
    private Boolean enabled;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 数据权限范围
     */
    private Integer dataScope;

    /**
     * 用户角色权限集合
     */
    private Collection<SimpleGrantedAuthority> authorities;

    /**
     * 用途：创建 SysUserDetails 实例。
     * 
     * 构造函数：根据用户认证信息初始化用户详情对象
     *
     * @param user 用户认证信息对象 {@link UserAuthCredentials}
     * 返回：无。
     */
    public SysUserDetails(UserAuthCredentials user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = ObjectUtil.equal(user.getStatus(), 1);
        this.deptId = user.getDeptId();
        this.dataScope = user.getDataScope();

        // 初始化角色权限集合
        this.authorities = CollectionUtil.isNotEmpty(user.getRoles())
                ? user.getRoles().stream()
                // 角色名加上前缀 "ROLE_"，用于区分角色 (ROLE_ADMIN) 和权限 (user:add)
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
                .collect(Collectors.toSet())
                : Collections.emptySet();
    }

    /**
     * 显式返回账号启用状态，避免依赖 {@link UserDetails} 默认实现导致禁用态仍可登录。
     *
     * @return true 表示账号启用；false 表示账号禁用
     */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
