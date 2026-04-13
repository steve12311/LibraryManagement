package org.dwtech.framework.ai.vectorstore;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CatalogVectorRebuildService
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogVectorRebuildService {
    private final BookService bookService;
    private final CatalogVectorStoreService catalogVectorStoreService;

    /**
     * 用途：分页重建馆藏图书向量文档。
     *
     * @param batchSize 批大小
     * @return 返回结果
     */
    public CatalogVectorRebuildSummary rebuildCatalogVectors(int batchSize) {
        long pageNum = 1L;
        long scanned = 0L;
        long synced = 0L;
        long skipped = 0L;
        long failed = 0L;

        while (true) {
            Page<BookPO> page = bookService.page(
                    Page.of(pageNum, batchSize),
                    new LambdaQueryWrapper<BookPO>()
                            .isNotNull(BookPO::getIsbn)
                            .isNotNull(BookPO::getIntro)
                            .orderByAsc(BookPO::getIsbn)
            );
            List<BookPO> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (BookPO book : records) {
                scanned++;
                if (StrUtil.isBlank(book.getIsbn()) || StrUtil.isBlank(book.getIntro())) {
                    skipped++;
                    continue;
                }
                try {
                    catalogVectorStoreService.syncCatalogBook(
                            book.getIsbn(),
                            book.getName(),
                            book.getAuthor(),
                            book.getIntro()
                    );
                    synced++;
                } catch (RuntimeException exception) {
                    failed++;
                    log.warn(
                            "action=rebuild_catalog_vector result=failed isbn={} exceptionType={}",
                            book.getIsbn(),
                            exception.getClass().getSimpleName()
                    );
                }
            }
            if (pageNum >= page.getPages()) {
                break;
            }
            pageNum++;
        }

        return new CatalogVectorRebuildSummary(scanned, synced, skipped, failed);
    }

    /**
     * CatalogVectorRebuildSummary
     *
     * @param scanned 扫描数量
     * @param synced 成功同步数量
     * @param skipped 跳过数量
     * @param failed 失败数量
     */
    public record CatalogVectorRebuildSummary(long scanned, long synced, long skipped, long failed) {
    }
}
