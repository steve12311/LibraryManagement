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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/menus")
public class MenuController {
    private final MenuService menuService;

    @GetMapping
    public Result<List<MenuVO>> getMenus(MenuQuery queryParams) {
        List<MenuVO> menuList = menuService.listMenus(queryParams);
        return Result.success(menuList);
    }

    @GetMapping("/options")
    public Result<List<Option<Long>>> getMenuOptions(
            @RequestParam(required = false, defaultValue = "false", value = "onlyParent") boolean onlyParent
    ) {
        List<Option<Long>> menus = menuService.listMenuOptions(onlyParent);
        return Result.success(menus);
    }

    @GetMapping("/routes")
    public Result<List<RouteVO>> getCurrentUserRoutes() {
        List<RouteVO> routeList = menuService.listCurrentUserRoutes();
        return Result.success(routeList);
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('sys:menu:edit')")
    public Result<MenuForm> getMenuForm(@PathVariable("id") Long id) {
        MenuForm menu = menuService.getMenuForm(id);
        return Result.success(menu);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:add')")
    @RepeatSubmit
    public Result<?> addMenu(@RequestBody MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

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
