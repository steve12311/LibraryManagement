package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.framework.ai.vector.application.LibraryCatalogWriteService;
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
     * 分页查询图书库存列表。
     * 支持按 ISBN、书名、分类、出版社等条件筛选，展示库存数量和可借数量。
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('sys:stock:list')")
    public PageResult<StockPageVO> getStockPage(@Valid StockPageQuery queryParams) {
        IPage<StockPageVO> result = stockService.getStockPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 根据 ISBN 获取图书库存详情。
     * 返回图书信息及库存数量、可借数量等数据，供入库/出库操作时使用。
     *
     * @param isbn 图书国际标准书号
     */
    @GetMapping("/{isbn}")
    @PreAuthorize("@ss.hasPerm('sys:stock:view')")
    public Result<StockForm> getStock(@PathVariable("isbn") String isbn) {
        StockForm result = stockService.getStockFormData(isbn);
        return Result.success(result);
    }

    /**
     * 图书入库。
     * 增加指定 ISBN 图书的库存数量，同时写入 AI 搜索索引以便智能检索。ISBN 不存在时自动创建新图书记录。
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
     * 图书出库。
     * 减少指定 ISBN 图书的库存数量，出库前校验可出库数量是否充足，使用乐观锁防止超卖。
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
