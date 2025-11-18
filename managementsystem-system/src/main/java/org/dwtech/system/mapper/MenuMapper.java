package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.po.MenuPO;

import java.util.List;
import java.util.Set;

@Mapper
public interface MenuMapper extends BaseMapper<MenuPO> {
    List<MenuPO> getMenusByRoleCodes(Set<String> roleCodes);
}
