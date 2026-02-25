package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.BookPO;

@Mapper
public interface BookMapper extends BaseMapper<BookPO> {
}
