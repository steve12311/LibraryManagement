package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.form.BorrowForm;
import org.dwtech.common.core.entity.query.BorrowPageQuery;
import org.dwtech.common.core.entity.vo.BorrowVO;
import org.dwtech.system.service.BorrowService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/borrow")
public class BorrowController {
    private final BorrowService borrowService;

    @GetMapping("/page")
    public PageResult<BorrowVO> getBorrowPage(@Valid BorrowPageQuery borrowPageQuery) {
        IPage<BorrowVO> result = borrowService.getBorrowPage(borrowPageQuery);
        return PageResult.success(result);
    }

    @PostMapping
    public Result<?> saveBorrow(@Valid @RequestBody BorrowForm formData) {
        boolean result = borrowService.saveBorrow(formData);
        return Result.judge(result);
    }

    @PutMapping("/{borrowId}")
    public Result<?> updateBorrow(@PathVariable("borrowId") String borrowId, @Valid @RequestBody BorrowForm formData) {
        boolean result = borrowService.updateBorrow(borrowId, formData);
        return Result.judge(result);
    }
}
