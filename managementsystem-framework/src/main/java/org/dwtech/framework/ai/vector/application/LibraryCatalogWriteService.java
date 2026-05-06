package org.dwtech.framework.ai.vector.application;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.framework.ai.vector.queue.CatalogVectorSyncMessage;
import org.dwtech.framework.ai.vector.queue.CatalogVectorSyncPublisher;
import org.dwtech.framework.ai.vector.queue.CatalogVectorSyncTrigger;
import org.dwtech.system.file.queue.FileRefCountDeleteMessage;
import org.dwtech.system.file.queue.FileRefCountDeletePublisher;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.dwtech.system.util.FileUrlUtils;
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
    private final FileRefCountDeletePublisher fileRefCountDeletePublisher;

    /**
     * 更新图书信息，并在事务提交后发布向量同步消息。
     *
     * @param bookForm 图书表单
     * @return true 表示更新成功，false 表示更新失败
     */
    @Transactional
    public boolean updateBook(BookForm bookForm) {
        BookPO currentBook = bookService.getById(bookForm.getIsbn());
        String oldCoverUrl = (currentBook != null) ? currentBook.getCover() : null;

        boolean result = bookService.saveOrUpdateBook(bookForm);

        if (result) {
            handleCoverChange(oldCoverUrl, bookForm.getCover());
            publishVectorMessage(bookForm.getIsbn(), CatalogVectorSyncTrigger.BOOK_UPDATED);
        }
        return result;
    }

    /**
     * 新增库存，并仅在首次建档成功后发布向量同步消息。
     *
     * @param stockForm 库存表单
     * @return true 表示新增成功，false 表示新增失败
     */
    @Transactional
    public boolean addStock(StockForm stockForm) {
        StockAddResult result = stockService.addStock(stockForm);
        if (result.success() && result.firstStockIngested()) {
            publishVectorMessage(stockForm.getIsbn(), CatalogVectorSyncTrigger.FIRST_STOCK_INGESTED);
        }
        return result.success();
    }

    private void handleCoverChange(String oldCoverUrl, String newCoverUrl) {
        if (StrUtil.isNotBlank(oldCoverUrl) && !StrUtil.equals(oldCoverUrl, newCoverUrl)) {
            Long oldFileId = FileUrlUtils.extractFileId(oldCoverUrl);
            if (oldFileId != null) {
                fileRefCountDeletePublisher.publishAfterCommit(
                        FileRefCountDeleteMessage.initial(oldFileId)
                );
            }
        }
    }

    /**
     * 向 Redis Stream 发布馆藏向量同步消息（事务提交后执行）。
     *
     * @param isbn   图书 ISBN
     * @param trigger 触发来源
     */
    private void publishVectorMessage(String isbn, CatalogVectorSyncTrigger trigger) {
        catalogVectorSyncPublisher.publishAfterCommit(CatalogVectorSyncMessage.initial(isbn, trigger));
        log.info("action=publish_catalog_vector_queue result=scheduled isbn={} trigger={}", isbn, trigger);
    }
}
