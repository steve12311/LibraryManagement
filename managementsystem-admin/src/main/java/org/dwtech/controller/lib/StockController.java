package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.common.service.MilvusService;
import org.dwtech.common.utils.PrepareMilvusJson;
import org.dwtech.framework.ai.tools.VectorTool;
import org.dwtech.system.service.StockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final MilvusService milvusService;
    private final VectorTool vectorTool;
    private final PrepareMilvusJson prepareMilvusJson;

    /**
     * 用途：获取 stock page 信息。
     * 
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping("/page")
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
    public Result<?> addStock(@Valid @RequestBody StockForm stockForm) {
        boolean result = stockService.addStock(stockForm);
        if (stockForm.getIntro() != null && !stockForm.getIntro().isEmpty()) {
            List<float[]> vector = vectorTool.getVectors(List.of(stockForm.getIntro()));
            if (vector == null || vector.isEmpty()) {
                return Result.failed("向量为空");
            }
            milvusService.insertVectors(prepareMilvusJson.prepareInsertJson(stockForm.getIsbn(), vector.getFirst()));
        }
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
    public Result<?> outStock(@Valid @RequestBody StockForm stockForm) {
        boolean result = stockService.outStock(stockForm);
        return Result.judge(result);
    }
}
