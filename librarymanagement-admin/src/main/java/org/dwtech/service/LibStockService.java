package org.dwtech.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.LibBookStockDto;
import org.dwtech.common.enums.StockType;

public interface LibStockService {
    IPage<LibBookStockDto> selectStockList(LibBookStockDto libBookStockDto);

    Integer updateStock(StockType type, LibBookStockDto libBookStockDto);

    Integer deleteStock(Long[] ids);
}
