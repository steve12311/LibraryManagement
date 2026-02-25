package org.dwtech.framework.ai.tools;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.system.service.StockService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
/**
 * StockTool
 *
 * @author steve12311
 * @since 2026-02-22
 */

@RequiredArgsConstructor
public class StockTool {
    private final StockService stockService;
    @Tool(description = "通过ISBN码查询书籍库存")
    Result<List<StockPageVO>> findBookStockByISBNS(List<String> isbns) {
        return Result.success(stockService.getStockByExacts(isbns));
    }
}
