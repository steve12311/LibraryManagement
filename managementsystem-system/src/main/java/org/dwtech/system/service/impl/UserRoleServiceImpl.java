package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.po.UserRolePO;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.UserRoleMapper;
import org.dwtech.system.service.UserRoleService;
import org.dwtech.common.token.TokenManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRolePO> implements UserRoleService {
    private final TokenManager tokenManager;

    @Override
    @Transactional
    public void saveUserRoles(Long id, List<Long> roleIds) {
        if (id == null || CollectionUtil.isEmpty(roleIds)) {
            return;
        }

        // 获取现有角色
        List<Long> userRoleIds = this.list(new LambdaQueryWrapper<UserRolePO>()
                        .select(UserRolePO::getRoleId)
                        .eq(UserRolePO::getUserId, id))
                .parallelStream()
                .map(UserRolePO::getRoleId)
                .toList();

        // 使用Set提升对比效率
        Set<Long> oldRoles = new HashSet<>(userRoleIds);
        Set<Long> newRoles = new HashSet<>(roleIds);

        // 计算变更集
        Set<Long> addedRoles = new HashSet<>(newRoles);
        addedRoles.removeAll(oldRoles);

        Set<Long> removedRoles = new HashSet<>(oldRoles);
        removedRoles.removeAll(newRoles);

        boolean rolesChanged = !addedRoles.isEmpty() || !removedRoles.isEmpty();

        // 批量保存新增角色
        if (!addedRoles.isEmpty()) {
            this.saveBatch(addedRoles.stream()
                    .map(roleId -> new UserRolePO(id, roleId))
                    .collect(Collectors.toList()));
        }

        // 删除废弃角色
        if (!removedRoles.isEmpty()) {
            this.remove(new LambdaQueryWrapper<UserRolePO>()
                    .eq(UserRolePO::getUserId, id)
                    .in(UserRolePO::getRoleId, removedRoles));
        }

        // 当权限变更时清除登录态
        if (rolesChanged) {
            // 获取用户所有有效token（根据实际token存储实现）
            String accessToken = SecurityUtils.getTokenFromRequest();
            tokenManager.invalidateToken(accessToken);
        }
    }
}
