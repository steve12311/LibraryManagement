package org.dwtech.system.service.impl;

import org.dwtech.system.converter.StockConverter;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.mapper.StockMapper;
import org.dwtech.system.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplAtomicUpdateTest {

    @Mock
    private StockConverter stockConverter;

    @Mock
    private BookService bookService;

    @Mock
    private StockMapper stockMapper;

    private StockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockServiceImpl(stockConverter, bookService);
        ReflectionTestUtils.setField(stockService, "baseMapper", stockMapper);
    }

    @Test
    void shouldIncreaseExistingStockAtomically() {
        StockForm stockForm = buildStockForm("9787300000001", 3);
        StockBO stockBo = new StockBO();
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(3);
        StockPO existingStock = new StockPO();
        existingStock.setIsbn("9787300000001");
        existingStock.setStock(5);
        existingStock.setCurrentStock(4);

        when(stockConverter.toBo(stockForm)).thenReturn(stockBo);
        when(stockConverter.toPo(stockBo)).thenReturn(convertedStock);
        when(stockMapper.selectById("9787300000001")).thenReturn(existingStock);
        when(stockMapper.increaseStockAndCurrentStock("9787300000001", 3)).thenReturn(1);

        boolean result = stockService.addStock(stockForm);

        assertThat(result).isTrue();
        verify(stockMapper).increaseStockAndCurrentStock("9787300000001", 3);
        verify(bookService, never()).saveOrUpdate(any());
    }

    @Test
    void shouldUseConditionalAtomicUpdateWhenBorrowingOutStock() {
        StockForm stockForm = buildStockForm("9787300000001", 1);
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(1);

        when(stockConverter.toPo(stockForm)).thenReturn(convertedStock);
        when(stockMapper.decreaseCurrentStock("9787300000001", 1)).thenReturn(1);

        boolean result = stockService.borrowOut(stockForm);

        assertThat(result).isTrue();
        verify(stockMapper).decreaseCurrentStock("9787300000001", 1);
    }

    @Test
    void shouldRejectBorrowOutWhenCurrentStockInsufficient() {
        StockForm stockForm = buildStockForm("9787300000001", 1);
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(1);
        StockPO existingStock = new StockPO();
        existingStock.setIsbn("9787300000001");
        existingStock.setCurrentStock(0);

        when(stockConverter.toPo(stockForm)).thenReturn(convertedStock);
        when(stockMapper.decreaseCurrentStock("9787300000001", 1)).thenReturn(0);
        when(stockMapper.selectById("9787300000001")).thenReturn(existingStock);

        assertThatThrownBy(() -> stockService.borrowOut(stockForm))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("书籍数量不足");
    }

    @Test
    void shouldUseConditionalAtomicUpdateWhenOutStock() {
        StockForm stockForm = buildStockForm("9787300000001", 2);
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(2);

        when(stockConverter.toPo(stockForm)).thenReturn(convertedStock);
        when(stockMapper.decreaseStockAndCurrentStock("9787300000001", 2)).thenReturn(1);

        boolean result = stockService.outStock(stockForm);

        assertThat(result).isTrue();
        verify(stockMapper).decreaseStockAndCurrentStock("9787300000001", 2);
    }

    @Test
    void shouldIncreaseCurrentStockAtomicallyWhenReturningBook() {
        StockForm stockForm = buildStockForm("9787300000001", 1);
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(1);

        when(stockConverter.toPo(stockForm)).thenReturn(convertedStock);
        when(stockMapper.increaseCurrentStock("9787300000001", 1)).thenReturn(1);

        boolean result = stockService.borrowEnter(stockForm);

        assertThat(result).isTrue();
        verify(stockMapper).increaseCurrentStock("9787300000001", 1);
    }

    private StockForm buildStockForm(String isbn, Integer stock) {
        StockForm form = new StockForm();
        form.setIsbn(isbn);
        form.setStock(stock);
        return form;
    }
}
