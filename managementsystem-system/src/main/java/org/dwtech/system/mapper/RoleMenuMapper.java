package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.bo.RolePermsBO;
import org.dwtech.system.model.entity.RoleMenuPO;

import java.util.List;
import java.util.Set;
/**
 * 角色-菜单关联数据访问层，提供角色权限查询接口
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuPO> {
    /**
     * 根据角色编码查询关联的权限列表
     *
     * @return 权限列表
     */
    List<RolePermsBO> getRolePermsList(String roleCode);

    /**
     * 根据角色 ID 查询关联的菜单 ID 列表
     *
     * @return 菜单 ID 列表
     */
    List<Long> listMenuIdsByRoleId(Long roleId);

    /**
     * 根据角色编码集合查询对应的权限标识集合
     *
     * @return 权限标识集合
     */
    Set<String> listRolePerms(Set<String> roles);
}
