package org.dwtech.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.SysDeptDto;
import org.dwtech.common.core.entity.po.SysDeptPo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.SysDeptMapper;
import org.dwtech.system.service.SysDeptService;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SysDeptServiceImpl implements SysDeptService {
    private final SysDeptMapper sysDeptMapper;

    public SysDeptServiceImpl(SysDeptMapper sysDeptMapper) {
        this.sysDeptMapper = sysDeptMapper;
    }


    @Override
    public SysDeptDto selectDeptById(Long deptId) {
        SysDeptDto sysDeptDto = new SysDeptDto();
        BeanUtil.copyProperties(sysDeptMapper.selectDeptById(deptId), sysDeptDto);
        return sysDeptDto;
    }

    @Override
    public List<SysDeptDto> selectDeptByIds(Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return List.of();
        }
        List<SysDeptDto> sysDeptDtoList = new ArrayList<>();
        sysDeptMapper.selectDeptByIds(ids).forEach(sysDeptPo -> {
            SysDeptDto sysDeptDto = new SysDeptDto();
            BeanUtil.copyProperties(sysDeptPo, sysDeptDto);
            sysDeptDtoList.add(sysDeptDto);
        });
        return sysDeptDtoList;
    }

    @Override
    public IPage<SysDeptDto> selectDeptList(SysDeptDto sysDept) {
        SysDeptPo sysDeptPo = new SysDeptPo();
        BeanUtil.copyProperties(sysDept, sysDeptPo);

        Page<SysDeptDto> sysDeptDtoPage = new Page<>();
        IPage<SysDeptPo> sysDeptPoIPage = sysDeptMapper.selectDeptList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), sysDeptPo, PageUtils.getCondition());
        BeanUtil.copyProperties(sysDeptPoIPage, sysDeptDtoPage);

        List<SysDeptDto> sysDeptDtoList = new ArrayList<>();
        sysDeptPoIPage.getRecords().forEach(sysDeptPo1 -> {
            SysDeptDto sysDeptDto = new SysDeptDto();
            BeanUtil.copyProperties(sysDeptPo1, sysDeptDto);
            sysDeptDtoList.add(sysDeptDto);
        });
        sysDeptDtoPage.setRecords(sysDeptDtoList);
        return sysDeptDtoPage;
    }

    @Override
    public IPage<SysDeptDto> buildDeptTree(IPage<SysDeptDto> deptList) {
        Page<SysDeptDto> page = new Page<>();
        List<SysDeptDto> depts = deptList.getRecords();

        // 构建部门树
        List<SysDeptDto> treeDepts = buildTree(depts);

        page.setRecords(treeDepts);
        page.setTotal(deptList.getTotal());
        page.setSize(deptList.getSize());
        page.setCurrent(deptList.getCurrent());
        return page;
    }

    @Override
    public Integer insertDept(SysDeptDto sysDept) {
        SysDeptPo sysDeptPo = new SysDeptPo();
        BeanUtil.copyProperties(sysDept, sysDeptPo);
        sysDeptPo.setCreateBy(SecurityUtils.getUsername());
        return sysDeptMapper.insertDept(sysDeptPo);
    }

    @Override
    public Integer updateDept(SysDeptDto sysDept) {
        SysDeptPo sysDeptPo = new SysDeptPo();
        BeanUtil.copyProperties(sysDept, sysDeptPo);
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

    private List<SysDeptDto> buildTree(List<SysDeptDto> deptList) {
        List<SysDeptDto> treeList = new ArrayList<>();
        Map<Long, SysDeptDto> deptMap = new HashMap<>();

        // 将所有部门存入Map，key为deptId
        for (SysDeptDto dept : deptList) {
            deptMap.put(dept.getDeptId(), dept);
        }

        // 遍历部门列表，构建树形结构
        for (SysDeptDto dept : deptList) {
            Long parentId = dept.getParentId();
            if (parentId == 0) {
                // 根节点直接添加到树列表
                treeList.add(dept);
            } else {
                // 找到父节点，将当前部门添加到父节点的children中
                SysDeptDto parentDept = deptMap.get(parentId);
                if (parentDept != null) {
                    if (parentDept.getChildren() == null) {
                        parentDept.setChildren(new ArrayList<>());
                    }
                    parentDept.getChildren().add(dept);
                }
            }
        }

        // 对树形结构进行排序（按orderNum）
        sortTree(treeList);

        return treeList;
    }

    private void sortTree(List<SysDeptDto> treeList) {
        if (treeList == null || treeList.isEmpty()) {
            return;
        }

        // 对当前层级排序
        treeList.sort(Comparator.comparingInt(SysDeptDto::getOrderNum));

        // 递归对子节点排序
        for (SysDeptDto dept : treeList) {
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                sortTree(dept.getChildren());
            }
        }
    }
}
