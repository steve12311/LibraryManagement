package org.dwtech.controller.lib;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.form.BookForm;
import org.dwtech.system.service.BookService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/book")
public class BookController {
    private final BookService bookService;

    @GetMapping({"/{isbn}"})
    public Result<BookForm> getBookInfo(@PathVariable("isbn") String isbn) {
        BookForm bookForm = bookService.getBookByIsbn(isbn);
        return Result.success(bookForm);
    }

    @PutMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:edit')")
    public Result<?> updateBook(BookForm bookForm) {
        boolean result = bookService.updateBook(bookForm);
        return Result.judge(result);
    }
}
