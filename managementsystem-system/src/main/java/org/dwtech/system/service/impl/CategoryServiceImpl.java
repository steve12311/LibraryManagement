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
import org.dwtech.system.model.vo.CategoryOptionVO;
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
 * 分类管理服务实现。提供分类树构建、懒加载选项和下拉选项功能，
 * 部分查询方法结合 Spring Cache 进行结果缓存。
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryPO> implements CategoryService {
    private final CategoryConverter categoryConverter;

    /**
     * 查询分类树列表。按查询条件筛选后，先获取所有分类数据，然后递归构建树形结构。
     * 无查询条件时结果会被 Spring Cache 缓存。
     *
     * @param queryParams 查询参数（分类名称、状态）
     * @return 分类树列表
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
     * 按父节点查询直接子分类。管理端表格通过该方法逐级加载分类节点，避免一次性构建全量分类树。
     *
     * @param queryParams 查询参数（父节点 ID、状态）
     * @return 当前父节点下的直接子分类
     */
    @Override
    @Cacheable(
            cacheNames = "category",
            key = "'children:' + (#p0 == null || #p0.parentId == null ? 0 : #p0.parentId) + ':status:' + (#p0 == null || #p0.status == null ? 'all' : #p0.status)",
            sync = true
    )
    public List<CategoryVO> listCategoryChildren(CategoryQuery queryParams) {
        Long parentId = queryParams == null ? null : queryParams.getParentId();
        Integer status = queryParams == null ? null : queryParams.getStatus();
        Long targetParentId = ObjectUtil.defaultIfNull(parentId, SystemConstants.ROOT_NODE_ID);

        List<CategoryPO> children = this.list(new LambdaQueryWrapper<CategoryPO>()
                .select(CategoryPO::getId, CategoryPO::getParentId, CategoryPO::getTreePath, CategoryPO::getName, CategoryPO::getType, CategoryPO::getSort)
                .eq(CategoryPO::getParentId, targetParentId)
                .eq(ObjectUtil.isNotNull(status), CategoryPO::getVisible, status)
                .orderByAsc(CategoryPO::getSort)
                .orderByAsc(CategoryPO::getId)
        );
        if (CollectionUtil.isEmpty(children)) {
            return Collections.emptyList();
        }

        List<Long> childIds = children.stream()
                .map(CategoryPO::getId)
                .toList();
        Set<Long> nonLeafIds = listNonLeafParentIds(childIds, status);

        return children.stream()
                .map(item -> {
                    CategoryVO categoryVO = categoryConverter.toVo(item);
                    categoryVO.setHasChildren(nonLeafIds.contains(item.getId()));
                    return categoryVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询所有可见分类的下拉选项树。先将分类按父节点分组，然后从根节点递归构建树形选项。
     * 结果被 Spring Cache 缓存。
     *
     * @return 分类下拉选项树
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
     * 按父节点懒加载分类选项。只查询指定父节点下的直接子节点，并判断每个子节点是否为叶子节点。
     * 结果被 Spring Cache 缓存。
     *
     * @param parentId 父节点 ID，空值视为根节点
     * @return 当前层级子节点列表
     */
    @Override
    @Cacheable(cacheNames = "category", key = "'lazy:' + (#p0 == null ? 0 : #p0)", sync = true)
    public List<CategoryOptionVO> listCategoryLazyOptions(Long parentId) {
        Long targetParentId = ObjectUtil.defaultIfNull(parentId, SystemConstants.ROOT_NODE_ID);
        List<CategoryPO> children = this.list(new LambdaQueryWrapper<CategoryPO>()
                .select(CategoryPO::getId, CategoryPO::getName, CategoryPO::getSort, CategoryPO::getType)
                .eq(CategoryPO::getVisible, 1)
                .eq(CategoryPO::getParentId, targetParentId)
                .orderByAsc(CategoryPO::getSort)
                .orderByAsc(CategoryPO::getId)
        );
        if (CollectionUtil.isEmpty(children)) {
            return Collections.emptyList();
        }

        List<Long> childIds = children.stream()
                .map(CategoryPO::getId)
                .toList();

        Set<Long> nonLeafIds = listNonLeafParentIds(childIds, 1);

        return children.stream()
                .map(item -> new CategoryOptionVO(item.getId(), item.getType() + " " + item.getName(), !nonLeafIds.contains(item.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 按 ID 查询单个分类节点（用于前端回显）。先查询分类信息，再判断是否有子节点。
     * 结果被 Spring Cache 缓存。
     *
     * @param categoryId 分类 ID
     * @return 分类选项，含是否是叶子节点的标记
     */
    @Override
    @Cacheable(cacheNames = "category", key = "'node:' + #p0", condition = "#p0 != null", sync = true)
    public CategoryOptionVO getCategoryOptionById(Long categoryId) {
        if (ObjectUtil.isNull(categoryId)) {
            return null;
        }

        CategoryPO category = this.getOne(new LambdaQueryWrapper<CategoryPO>()
                .select(CategoryPO::getId, CategoryPO::getName, CategoryPO::getType)
                .eq(CategoryPO::getId, categoryId)
                .eq(CategoryPO::getVisible, 1)
                .last("LIMIT 1")
        );
        if (ObjectUtil.isNull(category)) {
            return null;
        }

        boolean hasChildren = this.exists(new LambdaQueryWrapper<CategoryPO>()
                .eq(CategoryPO::getVisible, 1)
                .eq(CategoryPO::getParentId, categoryId)
        );
        return new CategoryOptionVO(category.getId(), category.getType() + " " + category.getName(), !hasChildren);
    }

    /**
     * 递归构建分类树。从给定父节点出发，逐层构建 CategoryVO 并设置 children 子列表。
     *
     * @param parentId 父节点 ID
     * @param categoryChildrenMap 按父节点分组的分类映射
     * @return 子分类列表
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
     * 递归构建分类下拉选项树。使用 ArrayList 避免 Stream#toList 返回不可变列表影响 Redis 缓存。
     *
     * @param parentId 父节点 ID
     * @param categoryChildrenMap 按父节点分组的分类映射
     * @return 子选项列表
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
     * 查询给定节点集合中哪些节点仍有直接子分类。
     *
     * @param parentIds 待判断的父节点 ID 集合
     * @param status 状态筛选；空值表示不按状态过滤
     * @return 存在子分类的父节点 ID 集合
     */
    private Set<Long> listNonLeafParentIds(List<Long> parentIds, Integer status) {
        if (CollectionUtil.isEmpty(parentIds)) {
            return Collections.emptySet();
        }

        return this.list(new LambdaQueryWrapper<CategoryPO>()
                        .select(CategoryPO::getParentId)
                        .eq(ObjectUtil.isNotNull(status), CategoryPO::getVisible, status)
                        .in(CategoryPO::getParentId, parentIds)
                        .groupBy(CategoryPO::getParentId)
                ).stream()
                .map(CategoryPO::getParentId)
                .collect(Collectors.toSet());
    }

    /**
     * 将分类列表按父节点 ID 分组，避免递归阶段重复全量扫描。
     *
     * @param categories 分类列表
     * @return 父节点 ID 到子分类列表的映射
     */
    private Map<Long, List<CategoryPO>> groupCategoriesByParentId(List<CategoryPO> categories) {
        return CollectionUtil.emptyIfNull(categories)
                .stream()
                .collect(Collectors.groupingBy(CategoryPO::getParentId));
    }
}
