package org.dwtech.controller.lib;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.common.service.MilvusService;
import org.dwtech.common.utils.PrepareMilvusJson;
import org.dwtech.framework.ai.tools.VectorTool;
import org.dwtech.system.service.BookService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/book")
public class BookController {
    private final BookService bookService;
    private final MilvusService milvusService;
    private final VectorTool vectorTool;
    private final PrepareMilvusJson prepareMilvusJson;

    @GetMapping({"/{isbn}/form"})
    public Result<BookForm> getBookFormData(@PathVariable("isbn") String isbn) {
        BookForm bookForm = bookService.getBookByIsbn(isbn);
        return Result.success(bookForm);
    }

    @PutMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('sys:stock:edit')")
    public Result<?> updateBook(@Valid @RequestBody BookForm bookForm) {
        boolean result = bookService.saveOrUpdateBook(bookForm);
        List<float[]> vector = vectorTool.getVectors(List.of(bookForm.getIntro()));
        if (vector == null || vector.isEmpty()) {
            return Result.failed("向量为空");
        }
        milvusService.insertVectors(prepareMilvusJson.prepareInsertJson(bookForm.getIsbn(), vector.getFirst()));
        return Result.judge(result);
    }

    @GetMapping("/options")
    public Result<List<Option<Long>>> listBookOptions() {
        List<Option<Long>> list = bookService.listBookOptions();
        return Result.success(list);
    }
}
