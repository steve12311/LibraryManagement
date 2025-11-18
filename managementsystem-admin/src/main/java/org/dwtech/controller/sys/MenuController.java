package org.dwtech.controller.sys;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.MenuForm;
import org.dwtech.common.core.entity.query.MenuQuery;
import org.dwtech.common.core.entity.vo.MenuVO;
import org.dwtech.common.core.entity.vo.RouteVO;
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
}
