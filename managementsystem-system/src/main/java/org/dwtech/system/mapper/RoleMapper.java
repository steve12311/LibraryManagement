package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.RolePO;

import java.util.Set;
/**
 * 角色数据访问层，提供角色信息的查询接口
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface RoleMapper extends BaseMapper<RolePO> {
    /**
     * 查询指定角色集合中的最大数据权限范围
     *
     * @return 最大数据权限范围
     */
    Integer getMaximumDataScope(@Param("roles") Set<String> roles);
}
