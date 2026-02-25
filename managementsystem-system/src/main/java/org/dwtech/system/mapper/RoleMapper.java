package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.RolePO;

import java.util.Set;
/**
 * RoleMapper
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface RoleMapper extends BaseMapper<RolePO> {
    Integer getMaximumDataScope(@Param("roles") Set<String> roles);
}
