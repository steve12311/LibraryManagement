package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.MenuPO;

import java.util.List;
import java.util.Set;
/**
 * MenuMapper
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface MenuMapper extends BaseMapper<MenuPO> {
    /**
     * 用途：获取 menus by role codes 信息。
     * 
     * @param roleCodes role codes
     * @return 结果列表
     */
    List<MenuPO> getMenusByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}
