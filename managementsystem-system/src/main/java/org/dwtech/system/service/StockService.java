package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.model.vo.StockPageVO;

import java.util.List;
/**
 * StockService
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface StockService {
    /**
     * 用途：获取公开书目分页信息。
     *
     * @param queryParams query params
     * @return 分页结果
     */
    IPage<PublicBookPageVO> getPublicBookPage(@Valid PublicBookPageQuery queryParams);

    /**
     * 用途：获取 stock page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    IPage<StockPageVO> getStockPage(@Valid StockPageQuery queryParams);

    /**
     * 用途：获取 stock by exacts 信息。
     * 
     * @param isbns isbns
     * @return 结果列表
     */
    List<StockPageVO> getStockByExacts(List<String> isbns);

    /**
     * 用途：新增 stock。
     * 
     * @param stockForm stock form
     * @return 返回结果
     */
    StockAddResult addStock(@Valid StockForm stockForm);

    /**
     * 用途：执行 out stock 操作。
     * 
     * @param stockForm stock form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean outStock(@Valid StockForm stockForm);

    /**
     * 用途：执行 borrow out 操作。
     * 
     * @param stockForm stock form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean borrowOut(StockForm stockForm);

    /**
     * 用途：执行 borrow enter 操作。
     * 
     * @param stockForm stock form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean borrowEnter(StockForm stockForm);

    /**
     * 用途：获取 stock form data 信息。
     * 
     * @param isbn isbn
     * @return 返回结果
     */
    StockForm getStockFormData(String isbn);
}
