package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.po.PublishPO;
import org.dwtech.common.core.entity.query.PublishPageQuery;

@Mapper
public interface PublishMapper extends BaseMapper<PublishPO> {
    Page<PublishPO> getPublishPage(Page<PublishPO> page, @Param("queryParams") PublishPageQuery queryParams);
}
