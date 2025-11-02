package org.dwtech.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.SysDeptDto;
import org.dwtech.common.core.entity.po.SysDeptPo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.SysDeptMapper;
import org.dwtech.system.service.SysDeptService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysDeptServiceImpl implements SysDeptService {
    private final SysDeptMapper sysDeptMapper;

    public SysDeptServiceImpl(SysDeptMapper sysDeptMapper) {
        this.sysDeptMapper = sysDeptMapper;
    }


    @Override
    public SysDeptDto selectDeptById(Long deptId) {
        return BeanUtil.copyProperties(sysDeptMapper.selectDeptById(deptId), SysDeptDto.class);
    }

    @Override
    public List<SysDeptDto> selectDeptByIds(Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return List.of();
        }
        List<SysDeptDto> sysDeptDtoList = new ArrayList<>();
        sysDeptMapper.selectDeptByIds(ids).forEach(sysDeptPo -> {
            sysDeptDtoList.add(BeanUtil.copyProperties(sysDeptPo, SysDeptDto.class));
        });
        return sysDeptDtoList;
    }

    @Override
    public List<SysDeptDto> selectDeptList(SysDeptDto sysDept) {
        List<SysDeptDto> sysDeptDtoList = new ArrayList<>();
        List<SysDeptPo> sysDeptPoList = sysDeptMapper.selectDeptList(BeanUtil.copyProperties(sysDept, SysDeptPo.class), PageUtils.getCondition());
        sysDeptPoList.forEach(sysDeptPo -> sysDeptDtoList.add(convertToDto(sysDeptPo)));
        return sysDeptDtoList;
    }

    @Override
    public List<SysDeptDto> buildDeptTree(List<SysDeptDto> deptDtoList) {
        if (CollectionUtils.isEmpty(deptDtoList)) {
            return Collections.emptyList();
        }

        List<SysDeptDto> fullSysDeptDtoList = completeMissingParents(deptDtoList);
        Map<Long, List<SysDeptDto>> childrenMap = buildChildrenMap(fullSysDeptDtoList);

        List<SysDeptDto> rootNodes = fullSysDeptDtoList.stream()
                .filter(dto -> dto.getParentId() == 0L)
                .toList();

        for (SysDeptDto rootNode : rootNodes) {
            buildSubTree(rootNode, childrenMap);
        }

        return rootNodes;
    }

    @Override
    public Integer insertDept(SysDeptDto sysDept) {
        SysDeptPo sysDeptPo = BeanUtil.copyProperties(sysDept, SysDeptPo.class);
        sysDeptPo.setCreateBy(SecurityUtils.getUsername());
        return sysDeptMapper.insertDept(sysDeptPo);
    }

    @Override
    public Integer updateDept(SysDeptDto sysDept) {
        SysDeptPo sysDeptPo = BeanUtil.copyProperties(sysDept, SysDeptPo.class);
        sysDeptPo.setUpdateBy(SecurityUtils.getUsername());
        return sysDeptMapper.updateDept(sysDeptPo);
    }

    @Override
    public Integer deleteDept(Long[] deptIds) {
        if (ArrayUtils.isEmpty(deptIds)) {
            return 0;
        } else if (deptIds.length == 1) {
            return sysDeptMapper.deleteDeptById(deptIds[0]);
        }
        return 0;
    }

    private List<SysDeptDto> completeMissingParents(List<SysDeptDto> sysDeptDtoList) {
        Set<Long> existingIds = sysDeptDtoList.stream()
                .map(SysDeptDto::getDeptId)
                .collect(Collectors.toSet());

        Set<Long> missingParentIds = new HashSet<>();

        for (SysDeptDto dto : sysDeptDtoList) {
            Long parentId = dto.getParentId();
            if (parentId != 0L && !existingIds.contains(parentId)) {
                missingParentIds.add(parentId);
            }
        }

        if (missingParentIds.isEmpty()) {
            return new ArrayList<>(sysDeptDtoList);
        }

        Long[] missingIdsArray = missingParentIds.toArray(new Long[0]);
        List<SysDeptPo> missingParentsPo = sysDeptMapper.selectDeptByIds(missingIdsArray);

        List<SysDeptDto> missingParentsDto = missingParentsPo.stream()
                .map(this::convertToDto)
                .toList();

        List<SysDeptDto> completedParents = completeMissingParents(missingParentsDto);

        List<SysDeptDto> result = new ArrayList<>(sysDeptDtoList);
        result.addAll(completedParents);
        return result;
    }

    private Map<Long, List<SysDeptDto>> buildChildrenMap(List<SysDeptDto> sysDeptList) {
        return sysDeptList.stream()
                .collect(Collectors.groupingBy(SysDeptDto::getParentId));
    }

    private void buildSubTree(SysDeptDto parentNode, Map<Long, List<SysDeptDto>> childrenMap) {
        Long parentId = parentNode.getDeptId();
        List<SysDeptDto> children = childrenMap.get(parentId);

        if (children != null && !children.isEmpty()) {
            parentNode.setChildren(children);

            for (SysDeptDto child : children) {
                buildSubTree(child, childrenMap);
            }
        } else {
            parentNode.setChildren(Collections.emptyList());
        }
    }

    private SysDeptDto convertToDto(SysDeptPo deptPo) {
        SysDeptDto sysDeptDto = BeanUtil.copyProperties(deptPo, SysDeptDto.class);
        sysDeptDto.setChildren(null);
        return sysDeptDto;
    }
}
