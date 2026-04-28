package org.dwtech.controller.sys;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.MenuForm;
import org.dwtech.system.model.query.MenuQuery;
import org.dwtech.system.model.vo.MenuVO;
import org.dwtech.system.model.vo.RouteVO;
import org.dwtech.system.service.MenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * MenuController
 *
 * @author steve12311
 * @since 2025-11-18
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/menus")
public class MenuController {
    private final MenuService menuService;

    /**
     * 查询菜单列表。
     * 支持按菜单名称、状态、可见性等条件筛选，返回树形菜单结构。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:list')")
    public Result<List<MenuVO>> getMenus(MenuQuery queryParams) {
        List<MenuVO> menuList = menuService.listMenus(queryParams);
        return Result.success(menuList);
    }

    /**
     * 查询菜单选项列表。
     * 可选是否仅返回父级菜单，用于前端菜单树或下拉选择器。
     *
     * @param onlyParent 是否仅返回父级菜单
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:menu:list')")
    public Result<List<Option<Long>>> getMenuOptions(
            @RequestParam(required = false, defaultValue = "false", value = "onlyParent") boolean onlyParent
    ) {
        List<Option<Long>> menus = menuService.listMenuOptions(onlyParent);
        return Result.success(menus);
    }

    /**
     * 获取当前登录用户的可访问路由列表。
     * 根据用户角色和权限动态生成前端侧边栏和路由配置。
     */
    @GetMapping("/routes")
    @PreAuthorize("isAuthenticated()")
    public Result<List<RouteVO>> getCurrentUserRoutes() {
        List<RouteVO> routeList = menuService.listCurrentUserRoutes();
        return Result.success(routeList);
    }

    /**
     * 根据 ID 获取菜单表单数据，用于编辑时回显。
     *
     * @param id 菜单 ID
     */
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    public Result<MenuForm> getMenuForm(@PathVariable("id") Long id) {
        MenuForm menu = menuService.getMenuForm(id);
        return Result.success(menu);
    }

    /**
     * 新增菜单。
     * 接收菜单表单数据，校验路由路径唯一性后写入数据库，支持设置父级菜单和权限标识。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:add')")
    @RepeatSubmit
    @OperLog(module = "菜单管理", action = "新增菜单", bizId = "#p0.routePath")
    public Result<?> addMenu(@RequestBody MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    /**
     * 更新菜单信息。
     * 根据路径 ID 和表单数据修改菜单属性，包含父菜单变更、权限标识更新等场景。
     *
     * @param menuId  菜单 ID
     * @param menuForm 菜单表单数据
     */
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    @RepeatSubmit
    @OperLog(module = "菜单管理", action = "修改菜单", bizId = "#p0")
    public Result<?> updateMenu(
            @PathVariable("id") Long menuId,
            @RequestBody MenuForm menuForm
    ) {
        menuForm.setId(menuId);
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    /**
     * 批量删除菜单。
     * 根据主键 ID 列表删除菜单记录，若菜单有子菜单则阻止删除。
     *
     * @param ids 菜单 ID 列表，多个以英文逗号分隔
     */
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:menu:delete')")
    @RepeatSubmit
    @OperLog(module = "菜单管理", action = "删除菜单", bizId = "#p0")
    public Result<?> deleteMenu(
            @PathVariable("ids") @Parameter(description = "菜单ID，多个以英文(,)分割") List<Long> ids
    ) {
        boolean result = menuService.deleteMenu(ids);
        return Result.judge(result);
    }
}
