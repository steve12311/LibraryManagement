package org.dwtech.framework.ai.tools;

import org.dwtech.common.core.entity.Result;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockToolTest {

    @Mock
    private StockService stockService;

    @Test
    void shouldReturnStockResultByIsbns() {
        List<String> isbns = List.of("9787111128069");
        StockPageVO stockPageVO = new StockPageVO();
        stockPageVO.setIsbn("9787111128069");
        List<StockPageVO> stocks = List.of(stockPageVO);
        when(stockService.getStockByExacts(isbns)).thenReturn(stocks);
        StockTool stockTool = new StockTool(stockService);

        Result<List<StockPageVO>> result = stockTool.findBookStockByISBNS(isbns);

        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getData()).containsExactly(stockPageVO);
        verify(stockService).getStockByExacts(isbns);
    }
}
