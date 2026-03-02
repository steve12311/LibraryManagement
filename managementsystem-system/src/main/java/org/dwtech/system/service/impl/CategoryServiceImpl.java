package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.constant.SystemConstants;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.CategoryPO;
import org.dwtech.system.model.query.CategoryQuery;
import org.dwtech.system.model.vo.CategoryVO;
import org.dwtech.system.converter.CategoryConverter;
import org.dwtech.system.mapper.CategoryMapper;
import org.dwtech.system.service.CategoryService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * CategoryServiceImpl
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryPO> implements CategoryService {
    private final CategoryConverter categoryConverter;

    /**
     * 用途：查询 categories 列表。
     * 
     * @param queryParams query params
     * @return 结果列表
     */
    @Override
    @Cacheable(
            cacheNames = "category",
            key = "'tree:all'",
            condition = "#p0 == null || (((#p0.categoryName == null) || (#p0.categoryName.trim().isEmpty())) && #p0.status == null)",
            sync = true
    )
    public List<CategoryVO> listCategories(CategoryQuery queryParams) {
        String categoryName = queryParams == null ? null : queryParams.getCategoryName();
        Integer status = queryParams == null ? null : queryParams.getStatus();

        List<CategoryPO> categories = this.list(new LambdaQueryWrapper<CategoryPO>()
                .select(CategoryPO::getId, CategoryPO::getParentId, CategoryPO::getTreePath, CategoryPO::getName, CategoryPO::getType, CategoryPO::getSort)
                .like(StrUtil.isNotBlank(categoryName), CategoryPO::getName, categoryName)
                .eq(ObjectUtil.isNotNull(status), CategoryPO::getVisible, status)
                .orderByAsc(CategoryPO::getSort)
                .orderByAsc(CategoryPO::getId)
        );

        if (CollectionUtil.isEmpty(categories)) {
            return Collections.emptyList();
        }

        // 获取所有分类ID
        Set<Long> categoryIds = categories.stream()
                .map(CategoryPO::getId)
                .collect(Collectors.toSet());

        // 获取根节点ID（递归的起点），即父节点ID中不包含在分类ID中的节点
        List<Long> rootIds = categories.stream()
                .map(CategoryPO::getParentId)
                .distinct()
                .filter(id -> !categoryIds.contains(id))
                .toList();

        Map<Long, List<CategoryPO>> categoryChildrenMap = groupCategoriesByParentId(categories);
        // 使用递归函数构建分类树
        return rootIds.stream()
                .flatMap(rootId -> buildCategoryTree(rootId, categoryChildrenMap).stream())
                .collect(Collectors.toList());
    }

    /**
     * 用途：查询 category options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    @Override
    @Cacheable(cacheNames = "category", key = "'options'")
    public List<Option<Long>> listCategoryOptions() {
        List<CategoryPO> list = this.list(new LambdaQueryWrapper<CategoryPO>()
                .select(CategoryPO::getId, CategoryPO::getParentId, CategoryPO::getName, CategoryPO::getSort)
                .eq(CategoryPO::getVisible, 1)
                .orderByAsc(CategoryPO::getSort)
                .orderByAsc(CategoryPO::getId)
        );
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, List<CategoryPO>> categoryChildrenMap = groupCategoriesByParentId(list);
        return buildCategoryOptions(SystemConstants.ROOT_NODE_ID, categoryChildrenMap);
    }

    /**
     * 用途：构建 category tree。
     * 
     * @param parentId parent ID
     * @param categoryChildrenMap categoryChildrenMap
     * @return 结果列表
     */
    private List<CategoryVO> buildCategoryTree(Long parentId, Map<Long, List<CategoryPO>> categoryChildrenMap) {
        List<CategoryPO> children = categoryChildrenMap.get(parentId);
        if (CollectionUtil.isEmpty(children)) {
            return Collections.emptyList();
        }
        return children.stream()
                .map(entity -> {
                    CategoryVO categoryVO = categoryConverter.toVo(entity);
                    List<CategoryVO> categoryChildren = buildCategoryTree(entity.getId(), categoryChildrenMap);
                    categoryVO.setChildren(categoryChildren);
                    return categoryVO;
                })
                .toList();
    }

    /**
     * 用途：构建 category options。
     * 
     * @param parentId parent ID
     * @param categoryChildrenMap categoryChildrenMap
     * @return 结果列表
     */
    private List<Option<Long>> buildCategoryOptions(Long parentId, Map<Long, List<CategoryPO>> categoryChildrenMap) {
        List<CategoryPO> children = categoryChildrenMap.get(parentId);
        if (CollectionUtil.isEmpty(children)) {
            return Collections.emptyList();
        }

        // 显式使用 ArrayList，避免 Stream#toList 返回不可变 ListN 导致 Redis 缓存类型信息不稳定
        List<Option<Long>> options = new ArrayList<>(children.size());
        for (CategoryPO category : children) {
            Option<Long> option = new Option<>(category.getId(), category.getName());
            List<Option<Long>> subOptions = buildCategoryOptions(category.getId(), categoryChildrenMap);
            if (CollectionUtil.isNotEmpty(subOptions)) {
                option.setChildren(subOptions);
            }
            options.add(option);
        }
        return options;
    }

    /**
     * 用途：按父节点分组分类数据，避免递归阶段重复全量扫描。
     *
     * @param categories 分类列表
     * @return 父节点到子分类的映射
     */
    private Map<Long, List<CategoryPO>> groupCategoriesByParentId(List<CategoryPO> categories) {
        return CollectionUtil.emptyIfNull(categories)
                .stream()
                .collect(Collectors.groupingBy(CategoryPO::getParentId));
    }
}
