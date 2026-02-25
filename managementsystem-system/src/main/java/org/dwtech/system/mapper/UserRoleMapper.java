package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.UserRolePO;

import java.util.Set;
/**
 * UserRoleMapper
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRolePO> {
    /**
     * 用途：查询 role ids by user id 列表。
     * 
     * @param userId user ID
     * @return 结果集合
     */
    Set<Long> listRoleIdsByUserId(Long userId);
    /**
     * 用途：统计 users for role。
     * 
     * @param roleId role ID
     * @return 数值结果
     */
    int countUsersForRole(Long roleId);
}
