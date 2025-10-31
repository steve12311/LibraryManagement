package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.Condition;
import org.dwtech.common.core.entity.po.SysRolePo;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRolePo> {
    List<SysRolePo> selectRolePermissionByUserId(Long userId);

    IPage<SysRolePo> selectRoleList(IPage<SysRolePo> page, @Param("sysRole") SysRolePo role, @Param("params") Condition condition);

    Integer insertRole(SysRolePo role);

    Integer updateRole(SysRolePo rolePo);

    Integer deleteRoleById(Long id);
}
