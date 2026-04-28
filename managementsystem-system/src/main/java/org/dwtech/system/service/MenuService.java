package org.dwtech.system.service;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.MenuForm;
import org.dwtech.system.model.query.MenuQuery;
import org.dwtech.system.model.vo.MenuVO;
import org.dwtech.system.model.vo.RouteVO;

import java.util.List;
/**
 * 菜单管理服务，提供菜单树查询、下拉选项、路由生成、菜单增删改功能。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface MenuService {
    /**
     * 查询菜单树列表。可按菜单名称和状态筛选，返回按排序字段排列的树形结构。
     *
     * @param queryParams 查询参数（菜单名称、状态）
     * @return 菜单树列表，含层级 children
     */
    List<MenuVO> listMenus(MenuQuery queryParams);

    /**
     * 查询菜单下拉选项列表。
     *
     * @param onlyParent 是否只查询父级菜单
     * @return 菜单选项列表
     */
    List<Option<Long>> listMenuOptions(boolean onlyParent);

    /**
     * 获取当前登录用户可见的路由列表，用于前端动态路由生成。
     *
     * @return 路由列表
     */
    List<RouteVO> listCurrentUserRoutes();

    /**
     * 根据 ID 查询菜单表单数据（用于编辑回显）。
     *
     * @param id 菜单主键 ID
     * @return 菜单表单
     */
    MenuForm getMenuForm(Long id);

    /**
     * 新增菜单。
     *
     * @param menuForm 菜单表单（父级 ID、名称、路由、权限标识等）
     * @return true 表示新增成功，false 表示失败
     */
    boolean saveMenu(MenuForm menuForm);

    /**
     * 批量删除菜单。
     *
     * @param ids 待删除的菜单主键 ID 列表
     * @return true 表示全部删除成功，false 表示失败
     */
    boolean deleteMenu(List<Long> ids);
}
