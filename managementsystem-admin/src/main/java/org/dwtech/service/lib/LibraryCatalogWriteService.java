package org.dwtech.service.lib;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.service.MilvusService;
import org.dwtech.common.utils.PrepareMilvusJson;
import org.dwtech.framework.ai.tools.VectorTool;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LibraryCatalogWriteService
 *
 * @author steve12311
 * @since 2026-03-23
 */
@Service
@RequiredArgsConstructor
public class LibraryCatalogWriteService {
    private final BookService bookService;
    private final StockService stockService;
    private final MilvusService milvusService;
    private final VectorTool vectorTool;
    private final PrepareMilvusJson prepareMilvusJson;

    /**
     * 用途：更新图书并同步向量。
     *
     * @param bookForm 图书表单
     * @return 操作结果，true 表示成功，false 表示失败
     */
    public boolean updateBook(BookForm bookForm) {
        boolean result = bookService.saveOrUpdateBook(bookForm);
        if (result) {
            syncVector(bookForm.getIsbn(), bookForm.getIntro());
        }
        return result;
    }

    /**
     * 用途：新增库存并按需同步向量。
     *
     * @param stockForm 库存表单
     * @return 操作结果，true 表示成功，false 表示失败
     */
    public boolean addStock(StockForm stockForm) {
        boolean result = stockService.addStock(stockForm);
        if (result && StrUtil.isNotBlank(stockForm.getIntro())) {
            syncVector(stockForm.getIsbn(), stockForm.getIntro());
        }
        return result;
    }

    /**
     * 用途：生成并写入图书向量。
     *
     * @param isbn isbn
     * @param intro 图书简介
     * 返回：无。
     */
    private void syncVector(String isbn, String intro) {
        if (StrUtil.isBlank(intro)) {
            throw new BusinessException("向量为空");
        }
        List<float[]> vectors = vectorTool.getVectors(List.of(intro));
        if (vectors == null || vectors.isEmpty()) {
            throw new BusinessException("向量为空");
        }
        milvusService.insertVectors(prepareMilvusJson.prepareInsertJson(isbn, vectors.getFirst()));
    }
}
