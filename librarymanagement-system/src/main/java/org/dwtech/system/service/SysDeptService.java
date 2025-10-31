package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.SysDeptDto;

import java.util.List;

public interface SysDeptService {
    SysDeptDto selectDeptById(Long deptId);

    List<SysDeptDto> selectDeptByIds(Long[] ids);

    IPage<SysDeptDto> selectDeptList(SysDeptDto sysDept);

    IPage<SysDeptDto> buildDeptTree(IPage<SysDeptDto> deptList);

    Integer insertDept(SysDeptDto sysDept);

    Integer updateDept(SysDeptDto sysDept);

    Integer deleteDept(Long[] deptIds);
}
