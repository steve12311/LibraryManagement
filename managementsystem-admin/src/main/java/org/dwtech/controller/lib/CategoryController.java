package org.dwtech.controller.lib;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryVO;
import org.dwtech.system.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * CategoryController
 *
 * @author steve12311
 * @since 2026-02-12
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> listCategories(CategoryQuery queryParams) {
        List<CategoryVO> categoryList = categoryService.listCategories(queryParams);
        return Result.success(categoryList);
    }

    @GetMapping("/options")
    public Result<List<Option<Long>>> listCategoryOptions() {
        List<Option<Long>> list = categoryService.listCategoryOptions();
        return Result.success(list);
    }
}
