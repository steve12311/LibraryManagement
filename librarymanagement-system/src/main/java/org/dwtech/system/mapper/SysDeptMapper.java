package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.Condition;
import org.dwtech.common.core.entity.SysDept;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
    IPage<SysDept> selectDeptList(IPage<SysDept> page, @Param("sysDept") SysDept sysDept, @Param("params") Condition condition);
}
