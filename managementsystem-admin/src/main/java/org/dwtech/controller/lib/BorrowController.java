package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.query.BorrowPageQuery;
import org.dwtech.system.model.vo.BorrowVO;
import org.dwtech.system.service.BorrowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * BorrowController
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/borrow")
public class BorrowController {
    private final BorrowService borrowService;

    /**
     * 用途：获取 borrow page 信息。
     * 
     * @param borrowPageQuery borrow page query
     * @return 返回结果
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('lib:borrow:list')")
    public PageResult<BorrowVO> getBorrowPage(@Valid BorrowPageQuery borrowPageQuery) {
        IPage<BorrowVO> result = borrowService.getBorrowPage(borrowPageQuery);
        return PageResult.success(result);
    }

    /**
     * 用途：保存 borrow。
     * 
     * @param formData form data
     * @return 返回结果
     */
    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:borrow:add')")
    public Result<?> saveBorrow(@Valid @RequestBody BorrowForm formData) {
        boolean result = borrowService.saveBorrow(formData);
        return Result.judge(result);
    }

    /**
     * 用途：更新 borrow。
     * 
     * @param borrowId borrow ID
     * @param formData form data
     * @return 返回结果
     */
    @PutMapping("/{borrowId}")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:borrow:edit')")
    public Result<?> updateBorrow(@PathVariable("borrowId") String borrowId, @Valid @RequestBody BorrowForm formData) {
        boolean result = borrowService.updateBorrow(borrowId, formData);
        return Result.judge(result);
    }
}
