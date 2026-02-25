package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.CategoryPO;
/**
 * CategoryMapper
 *
 * @author steve12311
 * @since 2025-10-30
 */

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryPO> {
}
