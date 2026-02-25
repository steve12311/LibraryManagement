package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.system.model.bo.RolePermsBO;
import org.dwtech.system.model.entity.RoleMenuPO;
import org.dwtech.system.mapper.RoleMenuMapper;
import org.dwtech.system.service.RoleMenuService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
/**
 * RoleMenuServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenuPO> implements RoleMenuService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 用途：初始化 role perms cache。
     * 
     * 初始化权限缓存
     * 
     * 入参：无。
     * 返回：无。
     */
    @PostConstruct
    public void initRolePermsCache() {
        log.info("初始化权限缓存... ");
        refreshRolePermsCache();
    }

    /**
     * 用途：查询 menu ids by role id 列表。
     * 
     * @param roleId role ID
     * @return 结果列表
     */
    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return this.baseMapper.listMenuIdsByRoleId(roleId);
    }

    /**
     * 用途：刷新 role perms cache。
     * 
     * 刷新权限缓存
     * 
     * 入参：无。
     * 返回：无。
     */
    @Override
    public void refreshRolePermsCache() {
        // 清理所有角色权限缓存（Hash#delete 不支持通配符）
        clearAllRolePermsCache();

        List<RolePermsBO> list = this.baseMapper.getRolePermsList(null);
        if (CollectionUtil.isNotEmpty(list)) {
            list.forEach(item -> {
                String roleCode = item.getRoleCode();
                Set<String> perms = item.getPerms();
                if (CollectionUtil.isNotEmpty(perms)) {
                    redisTemplate.opsForHash().put(RedisConstants.System.ROLE_PERMS, roleCode, perms);
                }
            });
        }
    }

    /**
     * 用途：刷新 role perms cache。
     * 
     * @param roleCode role code
     * 返回：无。
     */
    @Override
    public void refreshRolePermsCache(String roleCode) {
// 清理权限缓存
        redisTemplate.opsForHash().delete(RedisConstants.System.ROLE_PERMS, roleCode);

        List<RolePermsBO> list = this.baseMapper.getRolePermsList(roleCode);
        if (CollectionUtil.isNotEmpty(list)) {
            RolePermsBO rolePerms = list.getFirst();
            if (rolePerms == null) {
                return;
            }

            Set<String> perms = rolePerms.getPerms();
            if (CollectionUtil.isNotEmpty(perms)) {
                redisTemplate.opsForHash().put(RedisConstants.System.ROLE_PERMS, roleCode, perms);
            }
        }
    }

    /**
     * 用途：刷新 role perms cache。
     * 
     * @param oldRoleCode old role code
     * @param newRoleCode new role code
     * 返回：无。
     */
    @Override
    public void refreshRolePermsCache(String oldRoleCode, String newRoleCode) {
// 清理旧角色权限缓存
        redisTemplate.opsForHash().delete(RedisConstants.System.ROLE_PERMS, oldRoleCode);

        // 添加新角色权限缓存
        List<RolePermsBO> list = this.baseMapper.getRolePermsList(newRoleCode);
        if (CollectionUtil.isNotEmpty(list)) {
            RolePermsBO rolePerms = list.getFirst();
            if (rolePerms == null) {
                return;
            }

            Set<String> perms = rolePerms.getPerms();
            redisTemplate.opsForHash().put(RedisConstants.System.ROLE_PERMS, newRoleCode, perms);
        }
    }

    /**
     * 用途：获取 role perms by role codes 信息。
     * 
     * @param roles roles
     * @return 结果集合
     */
    @Override
    public Set<String> getRolePermsByRoleCodes(Set<String> roles) {
        return this.baseMapper.listRolePerms(roles);
    }

    /**
     * 用途：执行 clear all role perms cache 操作。
     * 
     * 清空角色权限缓存
     * 
     * 入参：无。
     * 返回：无。
     */
    private void clearAllRolePermsCache() {
        Set<Object> cacheRoleCodes = redisTemplate.opsForHash().keys(RedisConstants.System.ROLE_PERMS);
        if (CollectionUtil.isNotEmpty(cacheRoleCodes)) {
            redisTemplate.opsForHash().delete(RedisConstants.System.ROLE_PERMS, cacheRoleCodes.toArray());
        }
    }
}
