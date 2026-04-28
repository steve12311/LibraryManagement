package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.UserRolePO;

import java.util.Set;
/**
 * 用户-角色关联数据访问层，提供用户角色查询接口
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRolePO> {
    /**
     * 根据用户 ID 查询关联的角色 ID 集合
     *
     * @return 角色 ID 集合
     */
    Set<Long> listRoleIdsByUserId(Long userId);
    /**
     * 统计指定角色关联的用户数量
     *
     * @return 用户数量
     */
    int countUsersForRole(Long roleId);
}
