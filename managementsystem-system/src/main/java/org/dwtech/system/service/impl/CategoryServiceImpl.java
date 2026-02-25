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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryPO> implements CategoryService {
    private final CategoryConverter categoryConverter;

    @Override
    public List<CategoryVO> listCategories(CategoryQuery queryParams) {
        List<CategoryPO> categories = this.list(new LambdaQueryWrapper<CategoryPO>()
                .like(StrUtil.isNotBlank(queryParams.getCategoryName()), CategoryPO::getName, queryParams.getCategoryName())
                .eq(ObjectUtil.isNotNull(queryParams.getStatus()), CategoryPO::getVisible, queryParams.getStatus())
                .orderByAsc(CategoryPO::getSort)
        );

        if (CollectionUtil.isEmpty(categories)) {
            return Collections.emptyList();
        }

        // 获取所有分类ID
        Set<Long> categoryIds = categories.stream()
                .map(CategoryPO::getId)
                .collect(Collectors.toSet());

        // 获取所有父级ID
        Set<Long> parentIds = categories.stream()
                .map(CategoryPO::getParentId)
                .collect(Collectors.toSet());

        // 获取根节点ID（递归的起点），即父节点ID中不包含在部门ID中的节点，注意这里不能拿顶级菜单 O 作为根节点，因为菜单筛选的时候 O 会被过滤掉
        List<Long> rootIds = parentIds.stream()
                .filter(id -> !categoryIds.contains(id))
                .toList();

        // 使用递归函数来构建菜单树
        return rootIds.stream()
                .flatMap(rootId -> buildCategoryTree(rootId, categories).stream())
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = "category", key = "'options'")
    public List<Option<Long>> listCategoryOptions() {
        List<CategoryPO> list = this.list(new LambdaQueryWrapper<CategoryPO>()
                .eq(CategoryPO::getVisible, true)
                .orderByAsc(CategoryPO::getSort)
        );
        return buildCategoryOptions(SystemConstants.ROOT_NODE_ID, list);
    }

    private List<CategoryVO> buildCategoryTree(Long parentId, List<CategoryPO> categories) {
        return CollectionUtil.emptyIfNull(categories)
                .stream()
                .filter(category -> category.getParentId().equals(parentId))
                .map(entity -> {
                    CategoryVO categoryVO = categoryConverter.toVo(entity);
                    List<CategoryVO> children = buildCategoryTree(entity.getId(), categories);
                    categoryVO.setChildren(children);
                    return categoryVO;
                }).toList();
    }

    private List<Option<Long>> buildCategoryOptions(Long parentId, List<CategoryPO> categories) {
        List<Option<Long>> categoryOptions = new ArrayList<>();

        for (CategoryPO category : categories) {
            if (category.getParentId().equals(parentId)) {
                Option<Long> option = new Option<>(category.getId(), category.getName());
                List<Option<Long>> children = buildCategoryOptions(category.getId(), categories);
                if (!children.isEmpty()) {
                    option.setChildren(children);
                }
                categoryOptions.add(option);
            }
        }

        return categoryOptions;
    }
}
