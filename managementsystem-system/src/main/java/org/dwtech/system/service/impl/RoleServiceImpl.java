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
     * 查询给定角色集合中的最大数据权限范围。
     *
     * @param roles 角色编码集合
     * @return 最大数据权限值
     */
    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        return this.baseMapper.getMaximumDataScope(roles);
    }

    /**
     * 分页查询角色列表。
     *
     * <p>非超级管理员会自动过滤超级管理员角色，防止越权查看与操作。</p>
     *
     * @param queryParams 分页与关键字查询参数
     * @return 角色分页结果
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
     * 查询角色下拉选项。
     *
     * <p>结果按当前用户是否为超级管理员分别缓存，避免每次重复构造下拉数据。</p>
     *
     * @return 角色选项列表
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
     * 新增或更新角色信息。
     *
     * <p>会校验角色名与编码唯一性；若编辑时角色编码或状态发生变化，将刷新对应角色权限缓存。</p>
     *
     * @param roleForm 角色表单数据
     * @return {@code true} 表示保存成功
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
     * 获取角色表单详情。
     *
     * @param roleId 角色 ID
     * @return 角色表单对象
     */
    @Override
    public RoleForm getRoleForm(Long roleId) {
        RolePO entity = this.getById(roleId);
        return roleConverter.toForm(entity);
    }

    /**
     * 批量删除角色。
     *
     * <p>删除前会校验角色存在性和是否已分配给用户，删除成功后刷新角色权限缓存。</p>
     *
     * @param ids 逗号分隔的角色 ID 列表
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
     * 更新角色状态。
     *
     * <p>状态更新成功后会立即刷新角色权限缓存，保证鉴权结果与最新状态一致。</p>
     *
     * @param roleId 角色 ID
     * @param status 目标状态
     * @return {@code true} 表示更新成功
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
     * 查询角色已分配的菜单 ID 列表。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    @Override
    @Cacheable(cacheNames = "role", key = "'menuIds:' + #p0")
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuService.listMenuIdsByRoleId(roleId);
    }

    /**
     * 为角色重置并分配菜单权限。
     *
     * <p>方法在事务内先删后增，最后统一刷新角色权限缓存，避免权限数据出现中间态。</p>
     *
     * @param roleId 角色 ID
     * @param menuIds 菜单 ID 列表
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"role", "menu"}, allEntries = true)
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        RolePO role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
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
     * 为角色分配用户。
     *
     * @param roleId 角色 ID
     * @param userIds 用户 ID 列表
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
