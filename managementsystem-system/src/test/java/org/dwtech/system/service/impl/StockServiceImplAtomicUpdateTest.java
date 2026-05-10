package org.dwtech.system.service.impl;

import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.StockConverter;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.entity.BookshelfPO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.mapper.BookshelfMapper;
import org.dwtech.system.mapper.StockMapper;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplAtomicUpdateTest {

    @Mock
    private StockConverter stockConverter;

    @Mock
    private BookService bookService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private BookshelfMapper bookshelfMapper;

    private StockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockServiceImpl(stockConverter, bookService, recommendationService, bookshelfMapper);
        ReflectionTestUtils.setField(stockService, "baseMapper", stockMapper);
    }

    @Test
    void shouldUpsertStockAndEnsureBookMetadataWhenAddingStock() {
        StockForm stockForm = buildStockForm("9787300000001", 3);
        StockBO stockBo = new StockBO();
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(3);
        BookPO convertedBook = new BookPO();
        convertedBook.setIsbn("9787300000001");

        when(stockConverter.toBo(stockForm)).thenReturn(stockBo);
        when(stockConverter.toPo(stockBo)).thenReturn(convertedStock);
        when(stockConverter.toBookPo(stockBo)).thenReturn(convertedBook);
        when(stockMapper.upsertStock("9787300000001", 3, null)).thenReturn(1);
        when(bookService.saveBookIfAbsent(convertedBook)).thenReturn(true);

        StockAddResult result = stockService.addStock(stockForm);

        assertThat(result.success()).isTrue();
        assertThat(result.firstStockIngested()).isTrue();
        verify(stockMapper).selectById("9787300000001");
        verify(stockMapper).upsertStock("9787300000001", 3, null);
        verify(bookService).saveBookIfAbsent(convertedBook);
    }

    @Test
    void shouldValidateShelfCapacityWhenAddingStockWithShelf() {
        StockForm stockForm = buildStockForm("9787300000002", 2);
        stockForm.setShelfId(9L);
        StockBO stockBo = new StockBO();
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000002");
        convertedStock.setStock(2);
        BookPO convertedBook = new BookPO();
        convertedBook.setIsbn("9787300000002");
        BookshelfPO shelf = new BookshelfPO();
        shelf.setId(9L);
        shelf.setStatus(1);
        shelf.setCapacity(5);

        when(stockConverter.toBo(stockForm)).thenReturn(stockBo);
        when(stockConverter.toPo(stockBo)).thenReturn(convertedStock);
        when(stockMapper.selectById("9787300000002")).thenReturn(null);
        when(bookshelfMapper.selectById(9L)).thenReturn(shelf);
        when(bookshelfMapper.sumStockByShelfId(9L)).thenReturn(3);
        when(stockMapper.upsertStock("9787300000002", 2, 9L)).thenReturn(1);
        when(stockConverter.toBookPo(stockBo)).thenReturn(convertedBook);
        when(bookService.saveBookIfAbsent(convertedBook)).thenReturn(true);

        StockAddResult result = stockService.addStock(stockForm);

        assertThat(result.success()).isTrue();
        verify(stockMapper).upsertStock("9787300000002", 2, 9L);
    }

    @Test
    void shouldRejectUpdateShelfWhenShelfDisabled() {
        StockPO existingStock = new StockPO();
        existingStock.setIsbn("9787300000003");
        existingStock.setStock(2);
        BookshelfPO shelf = new BookshelfPO();
        shelf.setId(10L);
        shelf.setStatus(0);
        shelf.setCapacity(10);

        when(stockMapper.selectById("9787300000003")).thenReturn(existingStock);
        when(bookshelfMapper.selectById(10L)).thenReturn(shelf);

        assertThatThrownBy(() -> stockService.updateStockShelf("9787300000003", 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("停用书架不能绑定图书");
    }

    @Test
    void shouldRejectUpdateShelfWhenCapacityInsufficient() {
        StockPO existingStock = new StockPO();
        existingStock.setIsbn("9787300000004");
        existingStock.setStock(4);
        BookshelfPO shelf = new BookshelfPO();
        shelf.setId(11L);
        shelf.setStatus(1);
        shelf.setCapacity(5);

        when(stockMapper.selectById("9787300000004")).thenReturn(existingStock);
        when(bookshelfMapper.selectById(11L)).thenReturn(shelf);
        when(bookshelfMapper.sumStockByShelfId(11L)).thenReturn(2);

        assertThatThrownBy(() -> stockService.updateStockShelf("9787300000004", 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("书架容量不足");
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
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.USER_OPERATION_EXCEPTION);
        assertThatThrownBy(() -> stockService.borrowOut(stockForm))
                .isInstanceOf(BusinessException.class)
                .hasMessage("书籍数量不足");
    }

    @Test
    void shouldRejectBorrowOutWhenStockRecordMissing() {
        StockForm stockForm = buildStockForm("9787300000001", 1);
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(1);

        when(stockConverter.toPo(stockForm)).thenReturn(convertedStock);
        when(stockMapper.decreaseCurrentStock("9787300000001", 1)).thenReturn(0);
        when(stockMapper.selectById("9787300000001")).thenReturn(null);

        assertThatThrownBy(() -> stockService.borrowOut(stockForm))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.USER_RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> stockService.borrowOut(stockForm))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存记录不存在");
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

    @Test
    void shouldRejectBorrowEnterWhenStockRecordMissing() {
        StockForm stockForm = buildStockForm("9787300000001", 1);
        StockPO convertedStock = new StockPO();
        convertedStock.setIsbn("9787300000001");
        convertedStock.setStock(1);

        when(stockConverter.toPo(stockForm)).thenReturn(convertedStock);
        when(stockMapper.increaseCurrentStock("9787300000001", 1)).thenReturn(0);
        when(stockMapper.selectById("9787300000001")).thenReturn(null);

        assertThatThrownBy(() -> stockService.borrowEnter(stockForm))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.USER_RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> stockService.borrowEnter(stockForm))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存记录不存在");
    }

    private StockForm buildStockForm(String isbn, Integer stock) {
        StockForm form = new StockForm();
        form.setIsbn(isbn);
        form.setStock(stock);
        return form;
    }
}
