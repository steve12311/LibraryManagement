package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.PublishPO;
import org.dwtech.system.model.query.PublishPageQuery;
/**
 * PublishMapper
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper
public interface PublishMapper extends BaseMapper<PublishPO> {
    Page<PublishPO> getPublishPage(Page<PublishPO> page, @Param("queryParams") PublishPageQuery queryParams);
}
