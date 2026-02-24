package org.dwtech.framework.ai.tools;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.vo.StockPageVO;
import org.dwtech.system.service.StockService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

@RequiredArgsConstructor
public class StockTool {
    private final StockService stockService;
    @Tool(description = "通过ISBN码查询书籍库存")
    Result<List<StockPageVO>> findBookStockByISBNS(List<String> isbns) {
        return Result.success(stockService.getStockByExacts(isbns));
    }
}
