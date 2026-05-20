package org.dwtech.system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.mapper.ReservationMapper;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.model.enums.ReservationStatus;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.service.ReservationService;
import org.dwtech.system.service.StockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationTask {

    private final ReservationMapper reservationMapper;
    private final StockService stockService;
    private final ReservationService reservationService;

    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void expireOverdueReservations() {
        List<ReservationPO> expired = reservationMapper.selectExpiredReady();
        if (expired.isEmpty()) {
            return;
        }

        log.info("开始处理过期预约，共 {} 条", expired.size());
        for (ReservationPO reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED.getValue());
            reservationMapper.updateById(reservation);

            StockForm increment = new StockForm();
            increment.setIsbn(reservation.getIsbn());
            increment.setStock(1);
            stockService.borrowEnter(increment);

            reservationService.promoteQueue(reservation.getIsbn());
            log.info("预约 {} 已过期", reservation.getId());
        }
        log.info("过期预约处理完成");
    }
}
