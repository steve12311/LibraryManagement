package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.CategoryPO;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryOptionVO;
import org.dwtech.system.model.vo.CategoryVO;

import java.util.List;
/**
 * 图书分类管理服务，提供分类树查询、懒加载选项、下拉选项及分类回显功能。
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface CategoryService extends IService<CategoryPO> {
    /**
     * 查询分类树列表。可按分类名称和状态筛选，返回按排序字段排列的树形结构。
     *
     * @param queryParams 查询参数（分类名称、状态）
     * @return 分类树列表，含层级 children
     */
    List<CategoryVO> listCategories(CategoryQuery queryParams);

    /**
     * 按父节点查询直接子分类，供管理表格懒加载使用。
     *
     * @param queryParams 查询参数（父节点 ID、状态）
     * @return 当前父节点下的直接子分类，每项含是否存在子节点标记
     */
    List<CategoryVO> listCategoryChildren(CategoryQuery queryParams);

    /**
     * 查询所有可见分类的下拉选项树，供前端级联选择器使用。结果按分类排序字段排列。
     *
     * @return 分类下拉选项树
     */
    List<Option<Long>> listCategoryOptions();

    /**
     * 按父节点懒加载分类选项列表（一次性只加载当前层的子节点）。
     *
     * @param parentId 父节点 ID，空值视为根节点
     * @return 当前层级子节点列表，每项含是否为叶子节点的标记
     */
    List<CategoryOptionVO> listCategoryLazyOptions(Long parentId);

    /**
     * 按 ID 查询单个分类节点信息（用于前端回显）。
     *
     * @param categoryId 分类 ID
     * @return 单个分类选项，含是否为叶子节点标记
     */
    CategoryOptionVO getCategoryOptionById(Long categoryId);
}
