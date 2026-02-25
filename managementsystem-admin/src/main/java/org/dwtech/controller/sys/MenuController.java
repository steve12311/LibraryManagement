package org.dwtech.controller.sys;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
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
     * 用途：获取 menus 信息。
     * 
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping
    public Result<List<MenuVO>> getMenus(MenuQuery queryParams) {
        List<MenuVO> menuList = menuService.listMenus(queryParams);
        return Result.success(menuList);
    }

    /**
     * 用途：获取 menu options 信息。
     * 
     * @param onlyParent only parent
     * @return 返回结果
     */
    @GetMapping("/options")
    public Result<List<Option<Long>>> getMenuOptions(
            @RequestParam(required = false, defaultValue = "false", value = "onlyParent") boolean onlyParent
    ) {
        List<Option<Long>> menus = menuService.listMenuOptions(onlyParent);
        return Result.success(menus);
    }

    /**
     * 用途：获取 current user routes 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/routes")
    public Result<List<RouteVO>> getCurrentUserRoutes() {
        List<RouteVO> routeList = menuService.listCurrentUserRoutes();
        return Result.success(routeList);
    }

    /**
     * 用途：获取 menu form 信息。
     * 
     * @param id 主键 ID
     * @return 返回结果
     */
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    public Result<MenuForm> getMenuForm(@PathVariable("id") Long id) {
        MenuForm menu = menuService.getMenuForm(id);
        return Result.success(menu);
    }

    /**
     * 用途：新增 menu。
     * 
     * @param menuForm menu form
     * @return 返回结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:add')")
    @RepeatSubmit
    public Result<?> addMenu(@RequestBody MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    /**
     * 用途：更新 menu。
     * 
     * @param menuId menu ID
     * @param menuForm menu form
     * @return 返回结果
     */
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    @RepeatSubmit
    public Result<?> updateMenu(
            @PathVariable("id") Long menuId,
            @RequestBody MenuForm menuForm
    ) {
        menuForm.setId(menuId);
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    /**
     * 用途：删除 menu。
     * 
     * @param ids 主键 ID 列表
     * @return 返回结果
     */
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:menu:delete')")
    @RepeatSubmit
    public Result<?> deleteMenu(
            @PathVariable("ids") @Parameter(description = "菜单ID，多个以英文(,)分割") List<Long> ids
    ) {
        boolean result = menuService.deleteMenu(ids);
        return Result.judge(result);
    }
}
