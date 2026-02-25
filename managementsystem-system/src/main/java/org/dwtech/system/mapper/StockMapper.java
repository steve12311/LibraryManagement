package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.query.StockPageQuery;

import java.util.List;
/**
 * StockMapper
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper
public interface StockMapper extends BaseMapper<StockPO> {
    /**
     * 用途：获取 stock page 信息。
     * 
     * @param page page
     * @param queryParams query params
     * @return 分页结果
     */
    Page<StockBO> getStockPage(Page<StockBO> page, @Param("queryParams") StockPageQuery queryParams);

    /**
     * 用途：获取 stock by exacts 信息。
     * 
     * @param isbns isbns
     * @return 结果列表
     */
    List<StockBO> getStockByExacts(@Param("isbns") List<String> isbns);

    /**
     * 用途：执行 select stock by id 操作。
     * 
     * @param isbn isbn
     * @return 返回结果
     */
    StockBO selectStockById(String isbn);
}
