package org.dwtech.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.dwtech.common.core.entity.po.LibPublishPo;

import java.util.List;

@Mapper
public interface LibPublishMapper {
    List<LibPublishPo> selectPublishByIds(Long[] ids);
}
