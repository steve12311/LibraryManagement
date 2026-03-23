package org.dwtech.service.lib;

import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.service.MilvusService;
import org.dwtech.common.utils.PrepareMilvusJson;
import org.dwtech.framework.ai.tools.VectorTool;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private MilvusService milvusService;

    @Mock
    private VectorTool vectorTool;

    @Mock
    private PrepareMilvusJson prepareMilvusJson;

    private LibraryCatalogWriteService libraryCatalogWriteService;

    @BeforeEach
    void setUp() {
        libraryCatalogWriteService = new LibraryCatalogWriteService(
                bookService,
                stockService,
                milvusService,
                vectorTool,
                prepareMilvusJson
        );
    }

    @Test
    void shouldUpdateBookAndSyncVectorThroughServiceLayer() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setIntro("Spring Boot 图书简介");
        float[] vector = new float[]{1.0F, 2.0F};

        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);
        when(vectorTool.getVectors(List.of("Spring Boot 图书简介"))).thenReturn(List.of(vector));
        when(prepareMilvusJson.prepareInsertJson("9787300000001", vector)).thenReturn("{json}");

        boolean result = libraryCatalogWriteService.updateBook(bookForm);

        assertThat(result).isTrue();
        verify(bookService).saveOrUpdateBook(bookForm);
        verify(milvusService).insertVectors("{json}");
    }

    @Test
    void shouldRejectBookUpdateWhenVectorEmpty() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setIntro("Spring Boot 图书简介");

        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);
        when(vectorTool.getVectors(List.of("Spring Boot 图书简介"))).thenReturn(List.of());

        assertThatThrownBy(() -> libraryCatalogWriteService.updateBook(bookForm))
                .isInstanceOf(BusinessException.class)
                .hasMessage("向量为空");
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
        verify(vectorTool, never()).getVectors(List.of(" "));
        verify(milvusService, never()).insertVectors(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldAddStockAndSyncVectorWhenIntroPresent() {
        StockForm stockForm = new StockForm();
        stockForm.setIsbn("9787300000001");
        stockForm.setStock(3);
        stockForm.setIntro("库存入库简介");
        float[] vector = new float[]{1.0F, 2.0F};

        when(stockService.addStock(stockForm)).thenReturn(true);
        when(vectorTool.getVectors(List.of("库存入库简介"))).thenReturn(List.of(vector));
        when(prepareMilvusJson.prepareInsertJson("9787300000001", vector)).thenReturn("{json}");

        boolean result = libraryCatalogWriteService.addStock(stockForm);

        assertThat(result).isTrue();
        verify(stockService).addStock(stockForm);
        verify(milvusService).insertVectors("{json}");
    }
}
