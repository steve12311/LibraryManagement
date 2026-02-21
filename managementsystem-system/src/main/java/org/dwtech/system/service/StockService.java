package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.common.core.entity.form.StockForm;
import org.dwtech.common.core.entity.query.StockPageQuery;
import org.dwtech.common.core.entity.vo.StockPageVO;

import java.util.List;

public interface StockService {
    IPage<StockPageVO> getStockPage(@Valid StockPageQuery queryParams);

    List<StockPageVO> getStockByExacts(List<String> isbns);

    boolean addStock(@Valid StockForm stockForm);

    boolean outStock(@Valid StockForm stockForm);
}
