package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.po.UserRolePO;

import java.util.Set;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRolePO> {
    Set<Long> listRoleIdsByUserId(Long userId);
    int countUsersForRole(Long roleId);
}
