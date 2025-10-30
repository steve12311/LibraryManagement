package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.SysDept;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.system.mapper.SysDeptMapper;
import org.dwtech.system.service.SysDeptService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SysDeptServiceImpl implements SysDeptService {
    private final SysDeptMapper sysDeptMapper;

    public SysDeptServiceImpl(SysDeptMapper sysDeptMapper) {
        this.sysDeptMapper = sysDeptMapper;
    }


    @Override
    public IPage<SysDept> selectDeptList(SysDept sysDept) {
        return sysDeptMapper.selectDeptList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), sysDept, PageUtils.getCondition());
    }

    @Override
    public IPage<SysDept> buildDeptTree(IPage<SysDept> deptList) {
        Page<SysDept> page = new Page<>();
        List<SysDept> depts = deptList.getRecords();

        // 构建部门树
        List<SysDept> treeDepts = buildTree(depts);

        page.setRecords(treeDepts);
        page.setTotal(deptList.getTotal());
        page.setSize(deptList.getSize());
        page.setCurrent(deptList.getCurrent());
        return page;
    }

    private List<SysDept> buildTree(List<SysDept> deptList) {
        List<SysDept> treeList = new ArrayList<>();
        Map<Long, SysDept> deptMap = new HashMap<>();

        // 将所有部门存入Map，key为deptId
        for (SysDept dept : deptList) {
            deptMap.put(dept.getDeptId(), dept);
        }

        // 遍历部门列表，构建树形结构
        for (SysDept dept : deptList) {
            Long parentId = dept.getParentId();
            if (parentId == 0) {
                // 根节点直接添加到树列表
                treeList.add(dept);
            } else {
                // 找到父节点，将当前部门添加到父节点的children中
                SysDept parentDept = deptMap.get(parentId);
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

    private void sortTree(List<SysDept> treeList) {
        if (treeList == null || treeList.isEmpty()) {
            return;
        }

        // 对当前层级排序
        treeList.sort(Comparator.comparingInt(SysDept::getOrderNum));

        // 递归对子节点排序
        for (SysDept dept : treeList) {
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                sortTree(dept.getChildren());
            }
        }
    }
}
