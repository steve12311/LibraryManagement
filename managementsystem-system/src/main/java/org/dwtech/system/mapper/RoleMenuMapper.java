package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.bo.RolePermsBO;
import org.dwtech.common.core.entity.po.RoleMenuPO;

import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuPO> {
    List<RolePermsBO> getRolePermsList(String roleCode);

    List<Long> listMenuIdsByRoleId(Long roleId);

    Set<String> listRolePerms(Set<String> roles);
}
