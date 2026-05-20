package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.query.ReservationPageQuery;
import org.dwtech.system.model.vo.AdminReservationPageVO;
import org.dwtech.system.model.vo.ReservationQueueVO;
import org.dwtech.system.service.ReservationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminReservationController
 *
 * @author steve12311
 * @since 2026-05-20
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reservation")
public class AdminReservationController {

    private final ReservationService reservationService;

    /**
     * 分页查询所有预约记录（管理员视图）。
     * 支持按用户、ISBN、预约状态等多维筛选。
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('lib:reservation:list')")
    public PageResult<AdminReservationPageVO> getReservationPage(@Valid ReservationPageQuery queryParams) {
        IPage<AdminReservationPageVO> result = reservationService.getReservationPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 查询某本图书的预约队列。
     * 返回按预约时间排序的等待用户列表。
     */
    @GetMapping("/queue/{isbn}")
    @PreAuthorize("@ss.hasPerm('lib:reservation:list')")
    public Result<List<ReservationQueueVO>> getBookQueue(@PathVariable("isbn") String isbn) {
        List<ReservationQueueVO> result = reservationService.getBookReservationQueue(isbn);
        return Result.success(result);
    }

    /**
     * 确认取书。
     * 管理员确认预约用户已取书，释放预约并更新相关库存状态。
     */
    @PutMapping("/{id}/pickup")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:reservation:edit')")
    @OperLog(module = "预约管理", action = "确认取书", bizId = "#p0")
    public Result<?> confirmPickup(@PathVariable("id") String id) {
        boolean result = reservationService.confirmPickup(id);
        return Result.judge(result);
    }

    /**
     * 管理员取消预约。
     * 管理员可强制取消任何预约记录。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('lib:reservation:edit')")
    @OperLog(module = "预约管理", action = "取消预约", bizId = "#p0")
    public Result<?> adminCancel(@PathVariable("id") String id) {
        boolean result = reservationService.adminCancelReservation(id);
        return Result.judge(result);
    }
}
