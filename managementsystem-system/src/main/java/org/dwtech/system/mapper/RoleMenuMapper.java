package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.bo.RolePermsBO;
import org.dwtech.system.model.entity.RoleMenuPO;

import java.util.List;
import java.util.Set;
/**
 * RoleMenuMapper
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuPO> {
    /**
     * 用途：获取 role perms list 信息。
     * 
     * @param roleCode role code
     * @return 结果列表
     */
    List<RolePermsBO> getRolePermsList(String roleCode);

    /**
     * 用途：查询 menu ids by role id 列表。
     * 
     * @param roleId role ID
     * @return 结果列表
     */
    List<Long> listMenuIdsByRoleId(Long roleId);

    /**
     * 用途：查询 role perms 列表。
     * 
     * @param roles roles
     * @return 结果集合
     */
    Set<String> listRolePerms(Set<String> roles);
}
