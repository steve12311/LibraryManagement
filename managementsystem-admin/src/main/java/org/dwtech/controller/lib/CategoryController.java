package org.dwtech.controller.lib;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryOptionVO;
import org.dwtech.system.model.vo.CategoryVO;
import org.dwtech.system.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * 查询分类列表，支持按分类名称、层级等条件筛选。
     * 返回树形分类结构，用于图书分类管理界面展示。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('lib:category:list')")
    public Result<List<CategoryVO>> listCategories(CategoryQuery queryParams) {
        List<CategoryVO> categoryList = categoryService.listCategories(queryParams);
        return Result.success(categoryList);
    }

    /**
     * 查询全部分类选项列表。
     * 返回分类的 ID 和名称键值对，用于前端下拉选择器。
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('lib:category:list')")
    public Result<List<Option<Long>>> listCategoryOptions() {
        List<Option<Long>> list = categoryService.listCategoryOptions();
        return Result.success(list);
    }

    /**
     * 按父节点懒加载分类选项。
     * 仅返回指定父节点下的直接子分类，用于分类树逐级展开。
     *
     * @param parentId 父节点 ID，空值表示查询根级分类
     */
    @GetMapping("/options/lazy")
    @PreAuthorize("@ss.hasPerm('lib:category:list')")
    public Result<List<CategoryOptionVO>> listCategoryLazyOptions(@RequestParam(value = "parentId", required = false) Long parentId) {
        List<CategoryOptionVO> list = categoryService.listCategoryLazyOptions(parentId);
        return Result.success(list);
    }

    /**
     * 按 ID 查询单个分类节点，用于前端表单回显。
     *
     * @param categoryId 分类 ID
     */
    @GetMapping("/options/node/{categoryId}")
    @PreAuthorize("@ss.hasPerm('lib:category:view')")
    public Result<CategoryOptionVO> getCategoryOptionById(@PathVariable("categoryId") Long categoryId) {
        CategoryOptionVO node = categoryService.getCategoryOptionById(categoryId);
        return Result.success(node);
    }
}
