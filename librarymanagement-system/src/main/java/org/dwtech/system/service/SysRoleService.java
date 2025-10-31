package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.SysRoleDto;

import java.util.Set;

public interface SysRoleService {
    Set<String> selectRolePermissionByUserId(Long userId);

    IPage<SysRoleDto> selectRoleList(SysRoleDto role);

    Integer insertRole(SysRoleDto role);

    Integer updateRole(SysRoleDto role);

    Integer deleteRole(Long[] ids);
}
