package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.model.form.ReservationForm;
import org.dwtech.system.model.query.MyReservationPageQuery;
import org.dwtech.system.model.vo.ReservationPageVO;
import org.dwtech.system.service.ReservationService;
import org.springframework.web.bind.annotation.*;

/**
 * ReservationController
 *
 * @author steve12311
 * @since 2026-05-20
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservation")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 创建图书预约。
     * 用户登录后可对可借图书发起预约，系统校验预约条件后生成预约记录。
     */
    @PostMapping
    @RepeatSubmit
    public Result<?> createReservation(@RequestBody ReservationForm form) {
        Long userId = SecurityUtils.getUserId();
        boolean result = reservationService.createReservation(userId, form.getIsbn());
        return Result.judge(result);
    }

    /**
     * 分页查询当前用户的预约记录。
     * 支持按预约状态、ISBN 等条件筛选。
     */
    @GetMapping("/page")
    public PageResult<ReservationPageVO> getMyReservations(@Valid MyReservationPageQuery queryParams) {
        Long userId = SecurityUtils.getUserId();
        IPage<ReservationPageVO> result = reservationService.getUserReservationPage(userId, queryParams);
        return PageResult.success(result);
    }

    /**
     * 取消预约。
     * 仅允许取消未过期且未确认取书的预约记录。
     */
    @DeleteMapping("/{id}")
    public Result<?> cancelReservation(@PathVariable("id") String id) {
        Long userId = SecurityUtils.getUserId();
        boolean result = reservationService.cancelReservation(userId, id);
        return Result.judge(result);
    }
}
