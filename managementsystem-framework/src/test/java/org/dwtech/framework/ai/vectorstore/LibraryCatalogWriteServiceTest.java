package org.dwtech.framework.ai.vectorstore;

import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
    private CatalogVectorQueuePublisher catalogVectorQueuePublisher;

    private LibraryCatalogWriteService libraryCatalogWriteService;

    @BeforeEach
    void setUp() {
        libraryCatalogWriteService = new LibraryCatalogWriteService(
                bookService,
                stockService,
                catalogVectorQueuePublisher
        );
    }

    @Test
    void shouldPublishBookUpdatedMessageWhenUpdatingBookSuccessfully() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        when(bookService.saveOrUpdateBook(bookForm)).thenReturn(true);

        boolean result = libraryCatalogWriteService.updateBook(bookForm);

        assertThat(result).isTrue();
        ArgumentCaptor<CatalogVectorQueueMessage> captor = ArgumentCaptor.forClass(CatalogVectorQueueMessage.class);
        verify(catalogVectorQueuePublisher).publishAfterCommit(captor.capture());
        assertThat(captor.getValue().isbn()).isEqualTo("9787300000001");
        assertThat(captor.getValue().trigger()).isEqualTo(CatalogVectorQueueTrigger.BOOK_UPDATED);
        assertThat(captor.getValue().retryCount()).isZero();
    }

    @Test
    void shouldPublishFirstStockMessageOnlyWhenFirstIngested() {
        StockForm stockForm = new StockForm();
        stockForm.setIsbn("9787300000001");
        when(stockService.addStock(stockForm)).thenReturn(new StockAddResult(true, true));

        boolean result = libraryCatalogWriteService.addStock(stockForm);

        assertThat(result).isTrue();
        ArgumentCaptor<CatalogVectorQueueMessage> captor = ArgumentCaptor.forClass(CatalogVectorQueueMessage.class);
        verify(catalogVectorQueuePublisher).publishAfterCommit(captor.capture());
        assertThat(captor.getValue().isbn()).isEqualTo("9787300000001");
        assertThat(captor.getValue().trigger()).isEqualTo(CatalogVectorQueueTrigger.FIRST_STOCK_INGESTED);
        assertThat(captor.getValue().retryCount()).isZero();
    }

    @Test
    void shouldSkipPublishingWhenAddingRepeatedStock() {
        StockForm stockForm = new StockForm();
        stockForm.setIsbn("9787300000001");
        when(stockService.addStock(stockForm)).thenReturn(new StockAddResult(true, false));

        boolean result = libraryCatalogWriteService.addStock(stockForm);

        assertThat(result).isTrue();
        verify(catalogVectorQueuePublisher, never()).publishAfterCommit(org.mockito.ArgumentMatchers.any());
    }
}
