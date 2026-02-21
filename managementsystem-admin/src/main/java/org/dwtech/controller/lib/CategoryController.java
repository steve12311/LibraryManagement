package org.dwtech.controller.lib;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.query.CategoryQuery;
import org.dwtech.common.core.entity.vo.CategoryVO;
import org.dwtech.system.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> getCategory(CategoryQuery queryParams) {
        List<CategoryVO> categoryList = categoryService.listMenus(queryParams);
        return Result.success(categoryList);
    }
}
