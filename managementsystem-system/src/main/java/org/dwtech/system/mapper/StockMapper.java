package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.bo.StockBO;
import org.dwtech.common.core.entity.po.StockPO;
import org.dwtech.common.core.entity.query.StockPageQuery;

import java.util.List;

@Mapper
public interface StockMapper extends BaseMapper<StockPO> {
    Page<StockBO> getStockPage(Page<StockBO> page, @Param("queryParams") StockPageQuery queryParams);

    List<StockBO> getStockByExacts(@Param("isbns") List<String> isbns);

    StockBO selectStockById(String isbn);
}
