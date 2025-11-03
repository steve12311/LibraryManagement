package org.dwtech.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.LibBookStockDto;

import java.util.List;

public interface LibStockService {
    IPage<LibBookStockDto> selectStockList(LibBookStockDto libBookStockDto);
}
