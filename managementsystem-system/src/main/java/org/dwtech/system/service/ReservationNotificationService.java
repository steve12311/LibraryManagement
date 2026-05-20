package org.dwtech.system.service;

import org.dwtech.system.model.entity.ReservationPO;

public interface ReservationNotificationService {
    void notifyReady(ReservationPO reservation);
    void notifyExpiringSoon(ReservationPO reservation);
}
