package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.DeptPO;
/**
 * DeptMapper
 *
 * @author steve12311
 * @since 2025-10-30
 */

@Mapper
public interface DeptMapper extends BaseMapper<DeptPO> {
}
