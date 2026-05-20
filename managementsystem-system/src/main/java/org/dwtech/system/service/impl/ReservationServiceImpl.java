package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.uuid.UUID;
import org.dwtech.system.converter.ReservationConverter;
import org.dwtech.system.config.ReservationProperties;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.mapper.ReservationMapper;
import org.dwtech.system.mapper.StockMapper;
import org.dwtech.system.model.bo.MyReservationBO;
import org.dwtech.system.model.bo.ReservationBO;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.entity.ReservationPO;
import org.dwtech.system.model.enums.ReservationStatus;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.MyReservationPageQuery;
import org.dwtech.system.model.query.ReservationPageQuery;
import org.dwtech.system.model.vo.AdminReservationPageVO;
import org.dwtech.system.model.vo.ReservationPageVO;
import org.dwtech.system.model.vo.ReservationQueueVO;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.ReservationNotificationService;
import org.dwtech.system.service.ReservationService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final ReservationConverter reservationConverter;
    private final ReservationProperties reservationProperties;
    private final BookService bookService;
    private final StockService stockService;
    private final StockMapper stockMapper;
    private final BorrowMapper borrowMapper;
    private final ReservationNotificationService notificationService;

    @Override
    @Transactional
    public boolean createReservation(Long userId, String isbn) {
        // 1. Validate book exists
        BookForm bookForm = bookService.getBookByIsbn(isbn);
        if (bookForm == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "图书不存在");
        }

        // 2. Validate user active count < max
        int activeCount = reservationMapper.countActiveByUser(userId);
        if (activeCount >= reservationProperties.getMaxPerUser()) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "预约数量已达上限");
        }

        // 3. Validate no duplicate (user doesn't already have an active reservation for this isbn)
        LambdaQueryWrapper<ReservationPO> duplicateCheck = new LambdaQueryWrapper<ReservationPO>()
                .eq(ReservationPO::getUserId, userId)
                .eq(ReservationPO::getIsbn, isbn)
                .in(ReservationPO::getStatus,
                        ReservationStatus.PENDING.getValue(),
                        ReservationStatus.READY.getValue());
        if (reservationMapper.selectCount(duplicateCheck) > 0) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "已预约该图书");
        }

        // 4. Validate book reservation count < stock
        int activeByIsbn = reservationMapper.countActiveByIsbn(isbn);
        StockBO stockBO = stockMapper.selectStockById(isbn);
        if (stockBO == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "库存记录不存在");
        }
        if (activeByIsbn >= stockBO.getStock()) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "预约数量已达上限");
        }

        // 5. Create reservation
        ReservationPO reservation = new ReservationPO();
        reservation.setId(UUID.randomUUID().toString());
        reservation.setIsbn(isbn);
        reservation.setBookName(bookForm.getName());
        reservation.setUserId(userId);

        // 6. If current_stock > 0 -> READY + decrement stock. Else -> PENDING.
        if (stockBO.getCurrentStock() > 0) {
            reservation.setStatus(ReservationStatus.READY.getValue());
            reservation.setPickupDeadline(calculatePickupDeadline());

            // Decrement stock
            StockForm stockForm = new StockForm();
            stockForm.setIsbn(isbn);
            stockForm.setStock(1);
            stockService.borrowOut(stockForm);
        } else {
            reservation.setStatus(ReservationStatus.PENDING.getValue());
        }

        reservationMapper.insert(reservation);
        return true;
    }

    @Override
    @Transactional
    public boolean cancelReservation(Long userId, String reservationId) {
        ReservationPO reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_PERMISSION_EXCEPTION, "无权取消该预约");
        }
        doCancel(reservation);
        return true;
    }

    @Override
    @Transactional
    public boolean adminCancelReservation(String reservationId) {
        ReservationPO reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "预约记录不存在");
        }
        doCancel(reservation);
        return true;
    }

    private void doCancel(ReservationPO reservation) {
        ReservationStatus status = ReservationStatus.fromValue(reservation.getStatus());
        if (status != ReservationStatus.PENDING && status != ReservationStatus.READY) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "当前状态不允许取消");
        }

        // If READY: increment stock + promoteQueue
        if (status == ReservationStatus.READY) {
            StockForm stockForm = new StockForm();
            stockForm.setIsbn(reservation.getIsbn());
            stockForm.setStock(1);
            stockService.borrowEnter(stockForm);
            promoteQueue(reservation.getIsbn());
        }

        // Set CANCELLED
        ReservationPO update = new ReservationPO();
        update.setId(reservation.getId());
        update.setStatus(ReservationStatus.CANCELLED.getValue());
        reservationMapper.updateById(update);
    }

    @Override
    @Transactional
    public boolean confirmPickup(String reservationId) {
        ReservationPO reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "预约记录不存在");
        }

        ReservationStatus status = ReservationStatus.fromValue(reservation.getStatus());
        if (status != ReservationStatus.READY) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "当前状态不允许取书");
        }

        // Validate not past deadline
        if (reservation.getPickupDeadline() != null && reservation.getPickupDeadline().before(new Date())) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "预约已过期");
        }

        // Create borrow record directly (stock was already decremented when reservation became READY)
        String borrowId = UUID.randomUUID().toString();
        BookForm bookForm = bookService.getBookByIsbn(reservation.getIsbn());
        BorrowPO borrow = new BorrowPO();
        borrow.setId(borrowId);
        borrow.setIsbn(reservation.getIsbn());
        borrow.setBookName(bookForm != null ? bookForm.getName() : reservation.getBookName());
        borrow.setUserId(reservation.getUserId());
        borrow.setReturnTime(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
        borrowMapper.insert(borrow);

        // Set FULFILLED
        ReservationPO update = new ReservationPO();
        update.setId(reservation.getId());
        update.setStatus(ReservationStatus.FULFILLED.getValue());
        update.setBorrowId(borrowId);
        reservationMapper.updateById(update);

        return true;
    }

    @Override
    @Transactional
    public void promoteQueue(String isbn) {
        ReservationPO firstPending = reservationMapper.selectFirstPendingByIsbn(isbn);
        if (firstPending == null) {
            return;
        }

        // Try borrowOut - if stock is insufficient, skip promotion
        try {
            StockForm stockForm = new StockForm();
            stockForm.setIsbn(isbn);
            stockForm.setStock(1);
            stockService.borrowOut(stockForm);
        } catch (BusinessException e) {
            log.info("库存不足，无法提升预约队列: isbn={}, error={}", isbn, e.getMessage());
            return;
        }

        // Set READY + pickup_deadline + notifyReady
        Date pickupDeadline = calculatePickupDeadline();
        ReservationPO update = new ReservationPO();
        update.setId(firstPending.getId());
        update.setStatus(ReservationStatus.READY.getValue());
        update.setPickupDeadline(pickupDeadline);
        reservationMapper.updateById(update);

        firstPending.setStatus(ReservationStatus.READY.getValue());
        firstPending.setPickupDeadline(pickupDeadline);
        notificationService.notifyReady(firstPending);
    }

    @Override
    public IPage<ReservationPageVO> getUserReservationPage(Long userId, MyReservationPageQuery queryParams) {
        Page<MyReservationBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<MyReservationBO> result = reservationMapper.getMyReservationPage(page, userId, queryParams);
        return reservationConverter.toMyPageVo(result);
    }

    @Override
    public IPage<AdminReservationPageVO> getReservationPage(ReservationPageQuery queryParams) {
        Page<ReservationBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<ReservationBO> result = reservationMapper.getReservationPage(page, queryParams);
        return reservationConverter.toAdminPageVo(result);
    }

    @Override
    public List<ReservationQueueVO> getBookReservationQueue(String isbn) {
        List<ReservationBO> queue = reservationMapper.getBookReservationQueue(isbn);
        return queue.stream().map(reservationConverter::toQueueVo).toList();
    }

    private Date calculatePickupDeadline() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, reservationProperties.getPickupDays());
        return calendar.getTime();
    }
}
