package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.PublishPO;
import org.dwtech.system.model.query.PublishPageQuery;
/**
 * 出版社数据访问层，提供出版社信息的分页查询接口
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper
public interface PublishMapper extends BaseMapper<PublishPO> {
    /**
     * 分页查询出版社列表，支持多种筛选条件
     *
     * @return 分页结果
     */
    Page<PublishPO> getPublishPage(Page<PublishPO> page, @Param("queryParams") PublishPageQuery queryParams);
}
