package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.po.DeptPO;

@Mapper
public interface DeptMapper extends BaseMapper<DeptPO> {
}
