package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.BorrowBO;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.query.BorrowPageQuery;
/**
 * BorrowMapper
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Mapper
public interface BorrowMapper extends BaseMapper<BorrowPO> {
    /**
     * 用途：获取 borrow page 信息。
     * 
     * @param page page
     * @param queryParams query params
     * @return 分页结果
     */
    Page<BorrowBO> getBorrowPage(Page<BorrowBO> page, @Param("queryParams") BorrowPageQuery queryParams);
}
