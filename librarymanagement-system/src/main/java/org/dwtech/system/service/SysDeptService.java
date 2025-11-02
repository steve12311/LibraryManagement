package org.dwtech.system.service;

import org.dwtech.common.core.entity.dto.SysDeptDto;

import java.util.List;

public interface SysDeptService {
    SysDeptDto selectDeptById(Long deptId);

    List<SysDeptDto> selectDeptByIds(Long[] ids);

    List<SysDeptDto> selectDeptList(SysDeptDto sysDept);

    List<SysDeptDto> buildDeptTree(List<SysDeptDto> deptList);

    Integer insertDept(SysDeptDto sysDept);

    Integer updateDept(SysDeptDto sysDept);

    Integer deleteDept(Long[] deptIds);
}
