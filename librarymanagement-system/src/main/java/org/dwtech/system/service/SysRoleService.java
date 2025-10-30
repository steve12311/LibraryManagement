package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.SysRole;

import java.util.Set;

public interface SysRoleService {
    Set<String> selectRolePermissionByUserId(Long userId);

    IPage<SysRole> selectRoleList(SysRole role);
}
