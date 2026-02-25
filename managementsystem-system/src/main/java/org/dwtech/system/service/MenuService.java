package org.dwtech.system.service;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.MenuForm;
import org.dwtech.system.model.query.MenuQuery;
import org.dwtech.system.model.vo.MenuVO;
import org.dwtech.system.model.vo.RouteVO;

import java.util.List;

public interface MenuService {
    List<MenuVO> listMenus(MenuQuery queryParams);

    List<Option<Long>> listMenuOptions(boolean onlyParent);

    List<RouteVO> listCurrentUserRoutes();

    MenuForm getMenuForm(Long id);

    boolean saveMenu(MenuForm menuForm);

    boolean deleteMenu(List<Long> ids);
}
