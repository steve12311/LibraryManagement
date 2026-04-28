package org.dwtech.controller.lib;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.framework.ai.vector.application.LibraryCatalogWriteService;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.service.BookService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * BookController
 *
 * @author steve12311
 * @since 2026-02-22
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/book")
public class BookController {
    private final BookService bookService;
    private final LibraryCatalogWriteService libraryCatalogWriteService;

    /**
     * 根据 ISBN 获取图书表单数据。
     * 返回图书详细信息，供入库编辑或修改时回显。
     *
     * @param isbn 图书国际标准书号
     */
    @GetMapping({"/{isbn}/form"})
    @PreAuthorize("@ss.hasPerm('sys:stock:edit')")
    public Result<BookForm> getBookFormData(@PathVariable("isbn") String isbn) {
        BookForm bookForm = bookService.getBookByIsbn(isbn);
        return Result.success(bookForm);
    }

    /**
     * 更新图书信息。
     * 接收完整图书表单数据，同步更新图书主表和关联信息，并刷新 AI 搜索索引。
     */
    @PutMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:edit')")
    @OperLog(module = "图书管理", action = "修改图书", bizId = "#p0.isbn")
    public Result<?> updateBook(@Valid @RequestBody BookForm bookForm) {
        boolean result = libraryCatalogWriteService.updateBook(bookForm);
        return Result.judge(result);
    }

    /**
     * 查询图书选项列表。
     * 返回图书的键值对集合，用于前端下拉选择器（如关联图书时选择）。
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:stock:list')")
    public Result<List<Option<Long>>> listBookOptions() {
        List<Option<Long>> list = bookService.listBookOptions();
        return Result.success(list);
    }
}
