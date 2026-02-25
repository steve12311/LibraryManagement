package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.MenuPO;

import java.util.List;
import java.util.Set;

@Mapper
public interface MenuMapper extends BaseMapper<MenuPO> {
    List<MenuPO> getMenusByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}
