package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.SysDept;

public interface SysDeptService {
    IPage<SysDept> selectDeptList(SysDept sysDept);

    IPage<SysDept> buildDeptTree(IPage<SysDept> deptList);
}
