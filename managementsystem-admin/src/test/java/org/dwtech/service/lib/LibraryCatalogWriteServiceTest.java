package org.dwtech.service.lib;

import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.framework.ai.vectorstore.CatalogVectorStoreService;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogWriteServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private StockService stockService;

    @Mock
    private CatalogVectorStoreService catalogVectorStoreService;

    private LibraryCatalogWriteService libraryCatalogWriteService;

    @BeforeEach
    void setUp() {
        libraryCatalogWriteService = new LibraryCatalogWriteService(
                bookService,
                stockService,
                catalogVectorStoreService
        );
    }

    @Test
    void shouldUpdateBookAndSyncVectorThroughServiceLayer() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setName("Spring Boot 实战");
        bookForm.setAuthor("张三");
        bookForm.setIntro("Spring Boot 图书简介");

        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);

        boolean result = libraryCatalogWriteService.updateBook(bookForm);

        assertThat(result).isTrue();
        verify(bookService).saveOrUpdateBook(bookForm);
        verify(catalogVectorStoreService).syncCatalogBook(
                "9787300000001",
                "Spring Boot 实战",
                "张三",
                "Spring Boot 图书简介"
        );
    }

    @Test
    void shouldUpdateBookAndSkipVectorSyncWhenIntroBlank() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setIntro(" ");

        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);

        boolean result = libraryCatalogWriteService.updateBook(bookForm);

        assertThat(result).isTrue();
        verify(bookService).saveOrUpdateBook(bookForm);
        verify(catalogVectorStoreService, never()).syncCatalogBook(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldUpdateBookAndDegradeWhenVectorSyncThrowsBusinessException() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setName("Spring Boot 实战");
        bookForm.setAuthor("张三");
        bookForm.setIntro("Spring Boot 图书简介");

        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);
        doThrow(new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "向量同步失败"))
                .when(catalogVectorStoreService)
                .syncCatalogBook("9787300000001", "Spring Boot 实战", "张三", "Spring Boot 图书简介");

        boolean result = libraryCatalogWriteService.updateBook(bookForm);

        assertThat(result).isTrue();
        verify(bookService).saveOrUpdateBook(bookForm);
    }

    @Test
    void shouldUpdateBookAndDegradeWhenVectorSyncThrowsRuntimeException() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setName("Spring Boot 实战");
        bookForm.setAuthor("张三");
        bookForm.setIntro("Spring Boot 图书简介");

        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);
        doThrow(new IllegalStateException("milvus down"))
                .when(catalogVectorStoreService)
                .syncCatalogBook("9787300000001", "Spring Boot 实战", "张三", "Spring Boot 图书简介");

        boolean result = libraryCatalogWriteService.updateBook(bookForm);

        assertThat(result).isTrue();
        verify(bookService).saveOrUpdateBook(bookForm);
    }

    @Test
    void shouldAddStockAndSkipVectorSyncWhenIntroBlank() {
        StockForm stockForm = new StockForm();
        stockForm.setIsbn("9787300000001");
        stockForm.setStock(3);
        stockForm.setIntro(" ");

        when(stockService.addStock(stockForm)).thenReturn(true);

        boolean result = libraryCatalogWriteService.addStock(stockForm);

        assertThat(result).isTrue();
        verify(stockService).addStock(stockForm);
        verify(catalogVectorStoreService, never()).syncCatalogBook(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldAddStockAndSyncVectorWhenIntroPresent() {
        StockForm stockForm = new StockForm();
        stockForm.setIsbn("9787300000001");
        stockForm.setName("库存图书");
        stockForm.setAuthor("李四");
        stockForm.setStock(3);
        stockForm.setIntro("库存入库简介");

        when(stockService.addStock(stockForm)).thenReturn(true);

        boolean result = libraryCatalogWriteService.addStock(stockForm);

        assertThat(result).isTrue();
        verify(stockService).addStock(stockForm);
        verify(catalogVectorStoreService).syncCatalogBook(
                "9787300000001",
                "库存图书",
                "李四",
                "库存入库简介"
        );
    }
}
