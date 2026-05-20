package org.dwtech.system.service.impl;

import lombok.RequiredArgsConstructor;
import org.dwtech.system.message.MessageService;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.model.enums.BizType;
import org.dwtech.system.service.ReservationNotificationService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Primary
@Service
@RequiredArgsConstructor
public class MessageReservationNotificationService implements ReservationNotificationService {
    private final MessageService messageService;

    /** 预约到书通知：发送 RESERVATION_READY 消息 */
    @Override
    public void notifyReady(ReservationPO reservation) {
        Map<String, String> params = new HashMap<>();
        params.put("bookName", reservation.getBookName());
        params.put("pickupDeadline", reservation.getPickupDeadline().toString());
        messageService.send(reservation.getUserId(), BizType.RESERVATION_READY,
                reservation.getId(), params);
    }

    /** 预约即将到期通知：发送 RESERVATION_EXPIRING 消息 */
    @Override
    public void notifyExpiringSoon(ReservationPO reservation) {
        Map<String, String> params = new HashMap<>();
        params.put("bookName", reservation.getBookName());
        params.put("pickupDeadline", reservation.getPickupDeadline().toString());
        messageService.send(reservation.getUserId(), BizType.RESERVATION_EXPIRING,
                reservation.getId(), params);
    }
}
