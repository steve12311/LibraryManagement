package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.RoleForm;
import org.dwtech.common.core.entity.query.RolePageQuery;
import org.dwtech.common.core.entity.vo.RolePageVO;

import java.util.List;
import java.util.Set;

public interface RoleService {
    Integer getMaximumDataScope(Set<String> roles);

    Page<RolePageVO> getRolePage(RolePageQuery queryParams);

    List<Option<Long>> listRoleOptions();

    boolean saveRole(@Valid RoleForm roleForm);

    RoleForm getRoleForm(Long roleId);

    void deleteRoles(String ids);

    boolean updateRoleStatus(Long roleId, Integer status);

    List<Long> getRoleMenuIds(Long roleId);

    void assignMenusToRole(Long roleId, List<Long> menuIds);

    void assignUserToRole(Long roleId, List<Long> userIds);
}
