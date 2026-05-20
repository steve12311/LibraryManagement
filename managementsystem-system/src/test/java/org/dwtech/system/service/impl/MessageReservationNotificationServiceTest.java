package org.dwtech.system.service.impl;

import org.dwtech.system.message.MessageService;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.model.enums.BizType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageReservationNotificationServiceTest {

    @Mock
    private MessageService messageService;

    private MessageReservationNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new MessageReservationNotificationService(messageService);
    }

    @Test
    void shouldCallMessageServiceForNotifyReady() {
        ReservationPO reservation = new ReservationPO();
        reservation.setId("res-1");
        reservation.setUserId(1001L);
        reservation.setBookName("Spring Boot 实战");
        reservation.setPickupDeadline(new Date());

        notificationService.notifyReady(reservation);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messageService).send(eq(1001L), eq(BizType.RESERVATION_READY), eq("res-1"), paramsCaptor.capture());
        assertThat(paramsCaptor.getValue()).containsKey("bookName");
        assertThat(paramsCaptor.getValue()).containsKey("pickupDeadline");
    }

    @Test
    void shouldCallMessageServiceForNotifyExpiringSoon() {
        ReservationPO reservation = new ReservationPO();
        reservation.setId("res-2");
        reservation.setUserId(1002L);
        reservation.setBookName("Java 编程思想");
        reservation.setPickupDeadline(new Date());

        notificationService.notifyExpiringSoon(reservation);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messageService).send(eq(1002L), eq(BizType.RESERVATION_EXPIRING), eq("res-2"), paramsCaptor.capture());
        assertThat(paramsCaptor.getValue()).containsKey("bookName");
        assertThat(paramsCaptor.getValue()).containsKey("pickupDeadline");
    }
}
