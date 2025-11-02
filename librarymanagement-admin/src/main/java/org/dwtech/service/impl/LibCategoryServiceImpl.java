package org.dwtech.service.impl;

import cn.hutool.core.bean.BeanUtil;
import org.dwtech.common.core.entity.dto.LibCategoryDto;
import org.dwtech.common.core.entity.po.LibCategoryPo;
import org.dwtech.mapper.LibCategoryMapper;
import org.dwtech.service.LibCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LibCategoryServiceImpl implements LibCategoryService {
    private final LibCategoryMapper libCategoryMapper;

    public LibCategoryServiceImpl(LibCategoryMapper libCategoryMapper) {
        this.libCategoryMapper = libCategoryMapper;
    }

    @Override
    public List<LibCategoryDto> selectLibCategoryList(LibCategoryDto libCategoryDto) {
        List<LibCategoryDto> libCategoryDtoList = new ArrayList<>();
        List<LibCategoryPo> libCategoryPoList = libCategoryMapper.selectLibCategoryList(BeanUtil.copyProperties(libCategoryDto, LibCategoryPo.class));
        libCategoryPoList.forEach(item -> {
            libCategoryDtoList.add(convertToDto(item));
        });

        return libCategoryDtoList;
    }

    @Override
    public List<LibCategoryDto> buildCategoryTree(List<LibCategoryDto> categoryDtoList) {
        if (CollectionUtils.isEmpty(categoryDtoList)) {
            return Collections.emptyList();
        }

        // 补全缺失的父节点
        List<LibCategoryDto> fullCategoryList = completeMissingParents(categoryDtoList);

        // 构建父子关系映射
        Map<Long, List<LibCategoryDto>> childrenMap = buildChildrenMap(fullCategoryList);

        // 获取所有根节点（parentId = 0）
        List<LibCategoryDto> rootNodes = fullCategoryList.stream()
                .filter(dto -> dto.getParentId() == 0L)
                .toList();

        // 为每个根节点构建子树
        for (LibCategoryDto rootNode : rootNodes) {
            buildSubTree(rootNode, childrenMap);
        }

        return rootNodes;
    }

    /**
     * 补全缺失的父节点
     */
    private List<LibCategoryDto> completeMissingParents(List<LibCategoryDto> categoryDtoList) {
        Set<Long> existingIds = categoryDtoList.stream()
                .map(LibCategoryDto::getCategoryId)
                .collect(Collectors.toSet());

        Set<Long> missingParentIds = new HashSet<>();

        // 找出所有缺失的父节点ID
        for (LibCategoryDto dto : categoryDtoList) {
            Long parentId = dto.getParentId();
            if (parentId != 0L && !existingIds.contains(parentId)) {
                missingParentIds.add(parentId);
            }
        }

        if (missingParentIds.isEmpty()) {
            return new ArrayList<>(categoryDtoList);
        }

        // 从数据库查询缺失的父节点
        Long[] missingIdsArray = missingParentIds.toArray(new Long[0]);
        List<LibCategoryPo> missingParentsPo = libCategoryMapper.selectLibCategoryByIds(missingIdsArray);

        // 使用Hutool进行PO到DTO的转换
        List<LibCategoryDto> missingParentsDto = missingParentsPo.stream()
                .map(this::convertToDto)
                .toList();

        // 递归补全更高层级的父节点
        List<LibCategoryDto> completedParents = completeMissingParents(missingParentsDto);

        // 合并所有节点
        List<LibCategoryDto> result = new ArrayList<>(categoryDtoList);
        result.addAll(completedParents);
        return result;
    }

    /**
     * 构建父子关系映射表
     */
    private Map<Long, List<LibCategoryDto>> buildChildrenMap(List<LibCategoryDto> categoryList) {
        return categoryList.stream()
                .collect(Collectors.groupingBy(LibCategoryDto::getParentId));
    }

    /**
     * 递归构建子树
     */
    private void buildSubTree(LibCategoryDto parentNode, Map<Long, List<LibCategoryDto>> childrenMap) {
        Long parentId = parentNode.getCategoryId();
        List<LibCategoryDto> children = childrenMap.get(parentId);

        if (children != null && !children.isEmpty()) {
            // 为当前节点设置子节点
            parentNode.setChildren(children);

            // 递归处理每个子节点
            for (LibCategoryDto child : children) {
                buildSubTree(child, childrenMap);
            }
        } else {
            // 如果没有子节点，设置为空列表
            parentNode.setChildren(Collections.emptyList());
        }
    }

    /**
     * 使用Hutool进行PO转DTO
     */
    private LibCategoryDto convertToDto(LibCategoryPo po) {
        // 使用Hutool的BeanUtil进行属性拷贝
        LibCategoryDto dto = BeanUtil.copyProperties(po, LibCategoryDto.class);
        // 确保children为null（因为传入的DTO列表要求children为null）
        dto.setChildren(null);
        return dto;
    }

}
