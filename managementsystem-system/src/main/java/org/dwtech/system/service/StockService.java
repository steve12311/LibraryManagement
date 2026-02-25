package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.StockPageVO;

import java.util.List;
/**
 * StockService
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface StockService {
    IPage<StockPageVO> getStockPage(@Valid StockPageQuery queryParams);

    List<StockPageVO> getStockByExacts(List<String> isbns);

    boolean addStock(@Valid StockForm stockForm);

    boolean outStock(@Valid StockForm stockForm);

    boolean borrowOut(StockForm stockForm);

    boolean borrowEnter(StockForm stockForm);

    StockForm getStockFormData(String isbn);
}
