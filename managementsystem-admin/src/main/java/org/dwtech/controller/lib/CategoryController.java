package org.dwtech.controller.lib;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryOptionVO;
import org.dwtech.system.model.vo.CategoryVO;
import org.dwtech.system.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * 用途：查询 categories 列表。
     * 
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping
    public Result<List<CategoryVO>> listCategories(CategoryQuery queryParams) {
        List<CategoryVO> categoryList = categoryService.listCategories(queryParams);
        return Result.success(categoryList);
    }

    /**
     * 用途：查询 category options 列表。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/options")
    public Result<List<Option<Long>>> listCategoryOptions() {
        List<Option<Long>> list = categoryService.listCategoryOptions();
        return Result.success(list);
    }

    /**
     * 用途：按父节点懒加载 category options 列表。
     *
     * @param parentId 父节点 ID，空值视为根节点
     * @return 返回结果
     */
    @GetMapping("/options/lazy")
    public Result<List<CategoryOptionVO>> listCategoryLazyOptions(@RequestParam(value = "parentId", required = false) Long parentId) {
        List<CategoryOptionVO> list = categoryService.listCategoryLazyOptions(parentId);
        return Result.success(list);
    }

    /**
     * 用途：按 ID 查询单个 category option（用于前端回显）。
     *
     * @param categoryId 分类 ID
     * @return 返回结果
     */
    @GetMapping("/options/node/{categoryId}")
    public Result<CategoryOptionVO> getCategoryOptionById(@PathVariable("categoryId") Long categoryId) {
        CategoryOptionVO node = categoryService.getCategoryOptionById(categoryId);
        return Result.success(node);
    }
}
