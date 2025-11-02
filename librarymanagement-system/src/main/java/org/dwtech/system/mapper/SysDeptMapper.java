package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.Condition;
import org.dwtech.common.core.entity.po.SysDeptPo;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDeptPo> {
    List<SysDeptPo> selectDeptList(@Param("sysDept") SysDeptPo sysDept, @Param("params") Condition condition);

    SysDeptPo selectDeptById(Long deptId);

    List<SysDeptPo> selectDeptByIds(Long[] deptIds);

    Integer insertDept(SysDeptPo sysDeptPo);

    Integer updateDept(SysDeptPo sysDeptPo);

    Integer deleteDeptById(Long deptId);
}
