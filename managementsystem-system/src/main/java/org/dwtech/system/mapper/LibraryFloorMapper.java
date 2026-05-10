package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.LibraryFloorPO;

/**
 * 图书馆楼层数据访问层
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Mapper
public interface LibraryFloorMapper extends BaseMapper<LibraryFloorPO> {
}
