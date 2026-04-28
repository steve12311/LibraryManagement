package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.MenuPO;

import java.util.List;
import java.util.Set;
/**
 * 菜单数据访问层，提供菜单信息的查询接口
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface MenuMapper extends BaseMapper<MenuPO> {
    /**
     * 根据角色编码集合查询关联的菜单列表
     *
     * @return 菜单列表
     */
    List<MenuPO> getMenusByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}
