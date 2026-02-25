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
    Set<Long> listRoleIdsByUserId(Long userId);
    int countUsersForRole(Long roleId);
}
