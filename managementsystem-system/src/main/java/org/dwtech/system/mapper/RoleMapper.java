package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.po.RolePO;

import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapper<RolePO> {
    Integer getMaximumDataScope(@Param("roles") Set<String> roles);
}
