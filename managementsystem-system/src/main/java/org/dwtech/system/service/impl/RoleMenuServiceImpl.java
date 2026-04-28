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
 * 角色-菜单关联服务实现。提供菜单 ID 查询、权限缓存刷新功能，
 * 使用 Redis Hash 结构缓存角色权限，启动时自动初始化缓存。
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
     * 应用启动时自动初始化权限缓存，确保 Redis 中的角色权限数据是最新状态。
     */
    @PostConstruct
    public void initRolePermsCache() {
        log.info("初始化权限缓存... ");
        refreshRolePermsCache();
    }

    /**
     * 查询指定角色拥有的菜单 ID 列表，委托 Mapper 查询角色-菜单关联表。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return this.baseMapper.listMenuIdsByRoleId(roleId);
    }

    /**
     * 刷新所有角色的权限缓存。流程：清空全部 Hash 缓存 → 从数据库加载全部角色权限 → 逐条写入 Redis。
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
     * 刷新指定角色的权限缓存。流程：清理该角色的旧缓存 → 从数据库加载 → 写入 Redis。
     *
     * @param roleCode 角色编码
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
     * 刷新因角色编码变更导致的权限缓存。流程：清理旧编码的缓存 → 加载新编码的权限 → 写入 Redis。
     *
     * @param oldRoleCode 旧角色编码
     * @param newRoleCode 新角色编码
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
     * 根据角色编码集合查询对应的权限标识集合，委托 Mapper 查询。
     *
     * @param roles 角色编码集合
     * @return 权限标识集合
     */
    @Override
    public Set<String> getRolePermsByRoleCodes(Set<String> roles) {
        return this.baseMapper.listRolePerms(roles);
    }

    /**
     * 清空 Redis 中所有角色的权限 Hash 缓存。
     */
    private void clearAllRolePermsCache() {
        Set<Object> cacheRoleCodes = redisTemplate.opsForHash().keys(RedisConstants.System.ROLE_PERMS);
        if (CollectionUtil.isNotEmpty(cacheRoleCodes)) {
            redisTemplate.opsForHash().delete(RedisConstants.System.ROLE_PERMS, cacheRoleCodes.toArray());
        }
    }
}
