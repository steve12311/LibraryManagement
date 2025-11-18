package org.dwtech.system.service;

import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.MenuForm;
import org.dwtech.common.core.entity.query.MenuQuery;
import org.dwtech.common.core.entity.vo.MenuVO;
import org.dwtech.common.core.entity.vo.RouteVO;

import java.util.List;

public interface MenuService {
    List<MenuVO> listMenus(MenuQuery queryParams);

    List<Option<Long>> listMenuOptions(boolean onlyParent);

    List<RouteVO> listCurrentUserRoutes();

    MenuForm getMenuForm(Long id);
}
