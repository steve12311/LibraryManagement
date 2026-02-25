package org.dwtech.common.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.common.constant.RedisConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import java.util.*;

/**
 * SpringSecurity 权限校验
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Component("ss")
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 判断当前登录用户是否具备指定权限。
     *
     * <p>权限判定流程：</p>
     * <p>1) 参数为空直接拒绝；2) 超级管理员直接放行；3) 从当前用户角色集合对应的缓存权限中匹配；
     * 4) 使用 Spring 的 {@code simpleMatch} 支持通配符权限表达式。</p>
     *
     * @param requiredPerm 所需权限标识，例如 {@code system:user:add}
     * @return {@code true} 表示具备权限，{@code false} 表示无权限
     */
    public boolean hasPerm(String requiredPerm) {

        if (StrUtil.isBlank(requiredPerm)) {
            return false;
        }
        // 超级管理员放行
        if (SecurityUtils.isRoot()) {
            return true;
        }

        // 获取当前登录用户的角色编码集合
        Set<String> roleCodes = SecurityUtils.getRoles();
        if (CollectionUtil.isEmpty(roleCodes)) {
            return false;
        }

        // 获取当前登录用户的所有角色的权限列表
        Set<String> rolePerms = this.getRolePermsFromCache(roleCodes);
        if (CollectionUtil.isEmpty(rolePerms)) {
            return false;
        }
        // 判断当前登录用户的所有角色的权限列表中是否包含所需权限
        boolean hasPermission = rolePerms.stream()
                .anyMatch(rolePerm ->
                        // 匹配权限，支持通配符(* 等)
                        PatternMatchUtils.simpleMatch(rolePerm, requiredPerm)
                );

        if (!hasPermission) {
            log.error("用户无操作权限：{}", requiredPerm);
        }
        return hasPermission;
    }


    /**
     * 从 Redis 缓存中批量加载角色权限并合并去重。
     *
     * <p>方法通过 {@code multiGet} 一次性读取多个角色的权限集合，减少多次网络往返，提高鉴权性能。</p>
     *
     * @param roleCodes 角色编码集合
     * @return 角色对应的权限并集；当输入为空时返回空集合
     */
    public Set<String> getRolePermsFromCache(Set<String> roleCodes) {
        // 检查输入是否为空
        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptySet();
        }

        Set<String> perms = new HashSet<>();
        // 从缓存中一次性获取所有角色的权限
        Collection<Object> roleCodesAsObjects = new ArrayList<>(roleCodes);
        List<Object> rolePermsList = redisTemplate.opsForHash().multiGet(RedisConstants.System.ROLE_PERMS, roleCodesAsObjects);

        for (Object rolePermsObj : rolePermsList) {
            if (rolePermsObj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> rolePerms = (Set<String>) rolePermsObj;
                perms.addAll(rolePerms);
            }
        }

        return perms;
    }

    /**
     * 兼容旧方法名，内部委托给 {@link #getRolePermsFromCache(Set)}。
     *
     * @param roleCodes 角色编码集合
     * @return 角色对应的权限并集
     * @deprecated 请改用 {@link #getRolePermsFromCache(Set)}
     */
    @Deprecated
    public Set<String> getRolePermsFormCache(Set<String> roleCodes) {
        return getRolePermsFromCache(roleCodes);
    }

}
