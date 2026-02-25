package org.dwtech.system.service;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.MenuForm;
import org.dwtech.system.model.query.MenuQuery;
import org.dwtech.system.model.vo.MenuVO;
import org.dwtech.system.model.vo.RouteVO;

import java.util.List;
/**
 * MenuService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface MenuService {
    /**
     * 用途：查询 menus 列表。
     * 
     * @param queryParams query params
     * @return 结果列表
     */
    List<MenuVO> listMenus(MenuQuery queryParams);

    /**
     * 用途：查询 menu options 列表。
     * 
     * @param onlyParent only parent
     * @return 结果列表
     */
    List<Option<Long>> listMenuOptions(boolean onlyParent);

    /**
     * 用途：查询 current user routes 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<RouteVO> listCurrentUserRoutes();

    /**
     * 用途：获取 menu form 信息。
     * 
     * @param id 主键 ID
     * @return 返回结果
     */
    MenuForm getMenuForm(Long id);

    /**
     * 用途：保存 menu。
     * 
     * @param menuForm menu form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean saveMenu(MenuForm menuForm);

    /**
     * 用途：删除 menu。
     * 
     * @param ids 主键 ID 列表
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean deleteMenu(List<Long> ids);
}
