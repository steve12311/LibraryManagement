package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.form.StockForm;
import org.dwtech.common.core.entity.query.StockPageQuery;
import org.dwtech.common.core.entity.vo.StockPageVO;
import org.dwtech.system.service.StockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stock")
public class StockController {
    private final StockService stockService;

    @GetMapping
    public PageResult<StockPageVO> getStockPage(@Valid StockPageQuery queryParams) {
        IPage<StockPageVO> result = stockService.getStockPage(queryParams);
        return PageResult.success(result);
    }

    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:entry')")
    public Result<?> addStock(@Valid StockForm stockForm) {
        boolean result = stockService.addStock(stockForm);
        return Result.judge(result);
    }

    @PutMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:out')")
    public Result<?> outStock(@Valid StockForm stockForm) {
        boolean result = stockService.outStock(stockForm);
        return Result.judge(result);
    }
}
