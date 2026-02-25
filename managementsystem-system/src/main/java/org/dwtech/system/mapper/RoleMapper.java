package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.RolePO;

import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapper<RolePO> {
    Integer getMaximumDataScope(@Param("roles") Set<String> roles);
}
