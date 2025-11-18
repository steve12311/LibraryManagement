package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.bo.RolePermsBO;
import org.dwtech.common.core.entity.po.RoleMenuPO;

import java.util.List;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuPO> {
    List<RolePermsBO> getRolePermsList(String roleCode);
}
