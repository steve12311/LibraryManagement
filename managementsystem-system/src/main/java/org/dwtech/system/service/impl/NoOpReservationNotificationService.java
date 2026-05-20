package org.dwtech.system.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.service.ReservationNotificationService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NoOpReservationNotificationService implements ReservationNotificationService {

    @Override
    public void notifyReady(ReservationPO reservation) {
        log.info("预约 {} 已可取书，用户 {}", reservation.getId(), reservation.getUserId());
    }

    @Override
    public void notifyExpiringSoon(ReservationPO reservation) {
        log.info("预约 {} 即将过期，用户 {}", reservation.getId(), reservation.getUserId());
    }
}
