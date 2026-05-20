package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.system.model.query.MyReservationPageQuery;
import org.dwtech.system.model.query.ReservationPageQuery;
import org.dwtech.system.model.vo.AdminReservationPageVO;
import org.dwtech.system.model.vo.ReservationPageVO;
import org.dwtech.system.model.vo.ReservationQueueVO;
import java.util.List;

public interface ReservationService {

    boolean createReservation(Long userId, String isbn);

    boolean cancelReservation(Long userId, String reservationId);

    boolean adminCancelReservation(String reservationId);

    boolean confirmPickup(String reservationId);

    void promoteQueue(String isbn);

    IPage<ReservationPageVO> getUserReservationPage(Long userId, MyReservationPageQuery queryParams);

    IPage<AdminReservationPageVO> getReservationPage(ReservationPageQuery queryParams);

    List<ReservationQueueVO> getBookReservationQueue(String isbn);
}
