package org.dwtech.framework.ai.vector.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.framework.ai.vector.queue.CatalogVectorSyncMessage;
import org.dwtech.framework.ai.vector.queue.CatalogVectorSyncPublisher;
import org.dwtech.framework.ai.vector.queue.CatalogVectorSyncTrigger;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LibraryCatalogWriteService
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryCatalogWriteService {
    private final BookService bookService;
    private final StockService stockService;
    private final CatalogVectorSyncPublisher catalogVectorSyncPublisher;

    /**
     * 用途：更新图书并在事务提交后发布向量同步消息。
     *
     * @param bookForm 图书表单
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Transactional
    public boolean updateBook(BookForm bookForm) {
        boolean result = bookService.saveOrUpdateBook(bookForm);
        if (result) {
            publishVectorMessage(bookForm.getIsbn(), CatalogVectorSyncTrigger.BOOK_UPDATED);
        }
        return result;
    }

    /**
     * 用途：新增库存，并仅在首次建档成功后发布向量同步消息。
     *
     * @param stockForm 库存表单
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Transactional
    public boolean addStock(StockForm stockForm) {
        StockAddResult result = stockService.addStock(stockForm);
        if (result.success() && result.firstStockIngested()) {
            publishVectorMessage(stockForm.getIsbn(), CatalogVectorSyncTrigger.FIRST_STOCK_INGESTED);
        }
        return result.success();
    }

    /**
     * 用途：发布馆藏向量队列消息。
     *
     * @param isbn isbn
     * @param trigger 触发来源
     * 返回：无。
     */
    private void publishVectorMessage(String isbn, CatalogVectorSyncTrigger trigger) {
        catalogVectorSyncPublisher.publishAfterCommit(CatalogVectorSyncMessage.initial(isbn, trigger));
        log.info("action=publish_catalog_vector_queue result=scheduled isbn={} trigger={}", isbn, trigger);
    }
}
