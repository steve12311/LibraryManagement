package org.dwtech.system.service;

import org.dwtech.common.core.entity.SysMenu;
import org.dwtech.common.core.entity.vo.RouterVo;

import java.util.List;

public interface SysMenuService {
    List<SysMenu> selectMenuTreeByUserId(Long userId);
    List<RouterVo> buildMenus(List<SysMenu> menus);
}
