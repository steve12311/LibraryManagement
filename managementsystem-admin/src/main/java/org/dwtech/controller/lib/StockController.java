package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.service.lib.LibraryCatalogWriteService;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.system.service.StockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * StockController
 *
 * @author steve12311
 * @since 2026-02-22
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stock")
public class StockController {
    private final StockService stockService;
    private final LibraryCatalogWriteService libraryCatalogWriteService;

    /**
     * 用途：获取 stock page 信息。
     * 
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('sys:stock:list')")
    public PageResult<StockPageVO> getStockPage(@Valid StockPageQuery queryParams) {
        IPage<StockPageVO> result = stockService.getStockPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 用途：获取 stock 信息。
     * 
     * @param isbn isbn
     * @return 返回结果
     */
    @GetMapping("/{isbn}")
    @PreAuthorize("@ss.hasPerm('sys:stock:view')")
    public Result<StockForm> getStock(@PathVariable("isbn") String isbn) {
        StockForm result = stockService.getStockFormData(isbn);
        return Result.success(result);
    }

    /**
     * 用途：新增 stock。
     * 
     * @param stockForm stock form
     * @return 返回结果
     */
    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:entry')")
    @OperLog(module = "库存管理", action = "图书入库", bizId = "#p0.isbn")
    public Result<?> addStock(@Valid @RequestBody StockForm stockForm) {
        boolean result = libraryCatalogWriteService.addStock(stockForm);
        return Result.judge(result);
    }

    /**
     * 用途：执行 out stock 操作。
     * 
     * @param stockForm stock form
     * @return 返回结果
     */
    @PutMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:out')")
    @OperLog(module = "库存管理", action = "图书出库", bizId = "#p0.isbn")
    public Result<?> outStock(@Valid @RequestBody StockForm stockForm) {
        boolean result = stockService.outStock(stockForm);
        return Result.judge(result);
    }
}
