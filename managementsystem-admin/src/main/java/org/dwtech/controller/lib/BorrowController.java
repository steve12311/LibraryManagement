package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.annontation.OperLog;
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
     * 分页查询借阅记录。
     * 支持按借阅人、图书 ISBN、借阅状态、借阅日期范围等多维筛选。
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('lib:borrow:list')")
    public PageResult<BorrowVO> getBorrowPage(@Valid BorrowPageQuery borrowPageQuery) {
        IPage<BorrowVO> result = borrowService.getBorrowPage(borrowPageQuery);
        return PageResult.success(result);
    }

    /**
     * 新增借阅记录。
     * 预检图书库存是否充足，使用悲观锁扣减库存，创建借阅订单并记录操作日志。
     */
    @PostMapping
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:borrow:add')")
    @OperLog(module = "借阅管理", action = "新增借阅", bizId = "#p0.isbn")
    public Result<?> saveBorrow(@Valid @RequestBody BorrowForm formData) {
        boolean result = borrowService.saveBorrow(formData);
        return Result.judge(result);
    }

    /**
     * 更新借阅记录。
     * 支持修改借阅状态（借出/归还/续借等），涉及库存数量调整和借阅历史追溯。
     *
     * @param borrowId 借阅记录 ID
     * @param formData 借阅表单数据
     */
    @PutMapping("/{borrowId}")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:borrow:edit')")
    @OperLog(module = "借阅管理", action = "更新借阅", bizId = "#p0")
    public Result<?> updateBorrow(@PathVariable("borrowId") String borrowId, @Valid @RequestBody BorrowForm formData) {
        boolean result = borrowService.updateBorrow(borrowId, formData);
        return Result.judge(result);
    }
}
