package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.constant.SystemConstants;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.RoleForm;
import org.dwtech.system.model.entity.RoleMenuPO;
import org.dwtech.system.model.entity.RolePO;
import org.dwtech.system.model.query.RolePageQuery;
import org.dwtech.system.model.vo.RolePageVO;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.converter.RoleConverter;
import org.dwtech.system.mapper.RoleMapper;
import org.dwtech.system.service.RoleMenuService;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
/**
 * RoleServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RolePO> implements RoleService {
    private final RoleConverter roleConverter;
    private final RoleMenuService roleMenuService;
    private final UserRoleService userRoleService;

    /**
     * 用途：获取 maximum data scope 信息。
     * 
     * @param roles roles
     * @return 数值结果
     */
    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        return this.baseMapper.getMaximumDataScope(roles);
    }

    /**
     * 用途：获取 role page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    @Override
    public Page<RolePageVO> getRolePage(RolePageQuery queryParams) {
        // 查询参数
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        String keywords = queryParams.getKeywords();

        // 查询数据
        Page<RolePO> rolePage = this.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<RolePO>()
                        .and(StrUtil.isNotBlank(keywords),
                                wrapper ->
                                        wrapper.like(RolePO::getName, keywords)
                                                .or()
                                                .like(RolePO::getCode, keywords)
                        )
                        .ne(!SecurityUtils.isRoot(), RolePO::getCode, SystemConstants.ROOT_ROLE_CODE) // 非超级管理员不显示超级管理员角色
                        .orderByAsc(RolePO::getSort).orderByDesc(RolePO::getCreateTime).orderByDesc(RolePO::getUpdateTime)
        );

        // 实体转换
        return roleConverter.toPageVo(rolePage);
    }

    /**
     * 用途：查询 role options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    @Override
    @Cacheable(cacheNames = "role", key = "'options:' + T(org.dwtech.common.utils.SecurityUtils).isRoot()")
    public List<Option<Long>> listRoleOptions() {
        // 查询数据
        List<RolePO> roleList = this.list(new LambdaQueryWrapper<RolePO>()
                .ne(!SecurityUtils.isRoot(), RolePO::getCode, SystemConstants.ROOT_ROLE_CODE)
                .select(RolePO::getId, RolePO::getName)
                .orderByAsc(RolePO::getSort)
        );

        // 实体转换
        return roleConverter.toOptions(roleList);
    }

    /**
     * 用途：保存 role。
     * 
     * @param roleForm role form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = {"role", "menu"}, allEntries = true)
    public boolean saveRole(RoleForm roleForm) {
        Long roleId = roleForm.getId();

        // 编辑角色时，判断角色是否存在
        RolePO oldRole = null;
        if (roleId != null) {
            oldRole = this.getById(roleId);
            Assert.isTrue(oldRole != null, "角色不存在");
        }

        String roleCode = roleForm.getCode();
        long count = this.count(new LambdaQueryWrapper<RolePO>()
                .ne(roleId != null, RolePO::getId, roleId)
                .and(wrapper ->
                        wrapper.eq(RolePO::getCode, roleCode).or().eq(RolePO::getName, roleForm.getName())
                ));
        Assert.isTrue(count == 0, "角色名称或角色编码已存在，请修改后重试！");

        // 实体转换
        RolePO role = roleConverter.toPo(roleForm);

        boolean result = this.saveOrUpdate(role);
        if (result) {
            // 判断角色编码或状态是否修改，修改了则刷新权限缓存
            if (oldRole != null
                    && (
                    !StrUtil.equals(oldRole.getCode(), roleCode) ||
                            !ObjectUtil.equals(oldRole.getStatus(), roleForm.getStatus())
            )) {
                roleMenuService.refreshRolePermsCache(oldRole.getCode(), roleCode);
            }
        }
        return result;
    }

    /**
     * 用途：获取 role form 信息。
     * 
     * @param roleId role ID
     * @return 返回结果
     */
    @Override
    public RoleForm getRoleForm(Long roleId) {
        RolePO entity = this.getById(roleId);
        return roleConverter.toForm(entity);
    }

    /**
     * 用途：删除 roles。
     * 
     * @param ids 主键 ID 列表
     * 返回：无。
     */
    @Override
    @CacheEvict(cacheNames = {"role", "menu"}, allEntries = true)
    public void deleteRoles(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的角色ID不能为空");
        List<Long> roleIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        for (Long roleId : roleIds) {
            RolePO role = this.getById(roleId);
            Assert.isTrue(role != null, "角色不存在");

            // 判断角色是否被用户关联
            boolean isRoleAssigned = userRoleService.hasAssignedUsers(roleId);
            Assert.isTrue(!isRoleAssigned, "角色【{}】已分配用户，请先解除关联后删除", role.getName());

            boolean deleteResult = this.removeById(roleId);
            if (deleteResult) {
                // 删除成功，刷新权限缓存
                roleMenuService.refreshRolePermsCache(role.getCode());
            }
        }
    }

    /**
     * 用途：更新 role status。
     * 
     * @param roleId role ID
     * @param status status
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @CacheEvict(cacheNames = {"role", "menu"}, allEntries = true)
    public boolean updateRoleStatus(Long roleId, Integer status) {
        RolePO role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        role.setStatus(status);
        boolean result = this.updateById(role);
        if (result) {
            // 刷新角色的权限缓存
            roleMenuService.refreshRolePermsCache(role.getCode());
        }
        return result;
    }

    /**
     * 用途：获取 role menu ids 信息。
     * 
     * @param roleId role ID
     * @return 结果列表
     */
    @Override
    @Cacheable(cacheNames = "role", key = "'menuIds:' + #roleId")
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuService.listMenuIdsByRoleId(roleId);
    }

    /**
     * 用途：分配 menus to role。
     * 
     * @param roleId role ID
     * @param menuIds menu ID 列表
     * 返回：无。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"role", "menu"}, allEntries = true)
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        RolePO role = this.getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        // 删除角色菜单
        roleMenuService.remove(
                new LambdaQueryWrapper<RoleMenuPO>()
                        .eq(RoleMenuPO::getRoleId, roleId)
        );
        // 新增角色菜单
        if (CollectionUtil.isNotEmpty(menuIds)) {
            List<RoleMenuPO> roleMenus = menuIds
                    .stream()
                    .map(menuId -> new RoleMenuPO(roleId, menuId))
                    .toList();
            roleMenuService.saveBatch(roleMenus);
        }

        // 刷新角色的权限缓存
        roleMenuService.refreshRolePermsCache(role.getCode());
    }

    /**
     * 用途：分配 users to role。
     * 
     * @param roleId role ID
     * @param userIds user ID 列表
     * 返回：无。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"role", "menu"}, allEntries = true)
    public void assignUsersToRole(Long roleId, List<Long> userIds) {
        RolePO role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        userRoleService.assignUsersToRole(roleId, userIds);
    }
}
