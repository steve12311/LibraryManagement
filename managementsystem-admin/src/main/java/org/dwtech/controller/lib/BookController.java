package org.dwtech.controller.lib;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.service.lib.LibraryCatalogWriteService;
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
     * 用途：获取 book form data 信息。
     * 
     * @param isbn isbn
     * @return 返回结果
     */
    @GetMapping({"/{isbn}/form"})
    @PreAuthorize("@ss.hasPerm('sys:stock:edit')")
    public Result<BookForm> getBookFormData(@PathVariable("isbn") String isbn) {
        BookForm bookForm = bookService.getBookByIsbn(isbn);
        return Result.success(bookForm);
    }

    /**
     * 用途：更新 book。
     * 
     * @param bookForm book form
     * @return 返回结果
     */
    @PutMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:edit')")
    public Result<?> updateBook(@Valid @RequestBody BookForm bookForm) {
        boolean result = libraryCatalogWriteService.updateBook(bookForm);
        return Result.judge(result);
    }

    /**
     * 用途：查询 book options 列表。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:stock:list')")
    public Result<List<Option<Long>>> listBookOptions() {
        List<Option<Long>> list = bookService.listBookOptions();
        return Result.success(list);
    }
}
