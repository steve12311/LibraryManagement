package org.dwtech.framework.ai.vectorstore;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogVectorRebuildServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private CatalogVectorStoreService catalogVectorStoreService;

    @InjectMocks
    private CatalogVectorRebuildService catalogVectorRebuildService;

    @Test
    void shouldRebuildCatalogVectorsByPageAndCountSkippedAndFailedRecords() {
        BookPO validBook = new BookPO();
        validBook.setIsbn("9787300000001");
        validBook.setName("Spring Boot 实战");
        validBook.setAuthor("张三");
        validBook.setIntro("Spring Boot 图书简介");

        BookPO skippedBook = new BookPO();
        skippedBook.setIsbn("9787300000002");
        skippedBook.setIntro(" ");

        BookPO failedBook = new BookPO();
        failedBook.setIsbn("9787300000003");
        failedBook.setName("失败图书");
        failedBook.setAuthor("李四");
        failedBook.setIntro("失败简介");

        Page<BookPO> firstPage = Page.of(1, 2);
        firstPage.setRecords(List.of(validBook, skippedBook));
        firstPage.setTotal(3);

        Page<BookPO> secondPage = Page.of(2, 2);
        secondPage.setRecords(List.of(failedBook));
        secondPage.setTotal(3);

        when(bookService.page(any(Page.class), any())).thenReturn(firstPage, secondPage);
        lenient().doThrow(new BusinessException("向量同步失败"))
                .when(catalogVectorStoreService)
                .syncCatalogBook("9787300000003", "失败图书", "李四", "失败简介");

        CatalogVectorRebuildService.CatalogVectorRebuildSummary summary =
                catalogVectorRebuildService.rebuildCatalogVectors(2);

        assertThat(summary.scanned()).isEqualTo(3);
        assertThat(summary.synced()).isEqualTo(1);
        assertThat(summary.skipped()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(1);
        verify(catalogVectorStoreService).syncCatalogBook("9787300000001", "Spring Boot 实战", "张三", "Spring Boot 图书简介");
    }
}
