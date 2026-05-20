package org.dwtech.system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.mapper.MessageRecordMapper;
import org.dwtech.system.message.MessageService;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.enums.BizType;
import org.dwtech.system.service.BorrowNotificationService;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 借阅通知服务实现。
 * 定时任务：每天 08:00 扫描到期/逾期借阅，通过消息中心发送通知（去重）。
 * 管理员手动：无视去重，自动判断 OVERDUE 或 OVERDUE_REMINDER。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowNotificationServiceImpl implements BorrowNotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BorrowMapper borrowMapper;
    private final MessageRecordMapper messageRecordMapper;
    private final MessageService messageService;

    /** 定时任务入口：依次发送 3 天提醒、1 天提醒、逾期通知 */
    @Override
    public void sendScheduledNotifications() {
        sendDueSoonReminders(3);  // 剩余 3 天到期的借阅
        sendDueSoonReminders(1);  // 剩余 1 天到期的借阅
        sendOverdueNotifications(); // 已逾期的借阅
    }

    /**
     * 管理员手动发送提醒（无视去重）。
     * 自动判断：已逾期 → OVERDUE，未逾期 → OVERDUE_REMINDER。
     */
    @Override
    public void sendReminder(String borrowId) {
        BorrowPO borrow = borrowMapper.selectById(borrowId);
        if (borrow == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (borrow.getRealityReturnTime() != null) {
            throw new BusinessException("已归还的借阅无需提醒");
        }
        boolean isOverdue = borrow.getReturnTime().before(new java.util.Date());
        if (isOverdue) {
            Map<String, String> params = buildOverdueParams(borrow);
            messageService.send(borrow.getUserId(), BizType.OVERDUE, borrow.getId(), params);
        } else {
            long daysLeft = (borrow.getReturnTime().getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            Map<String, String> params = buildDueSoonParams(borrow, Math.max((int) daysLeft, 1));
            messageService.send(borrow.getUserId(), BizType.OVERDUE_REMINDER, borrow.getId(), params);
        }
        log.info("action=borrow_remind result=sent borrowId={} userId={} isOverdue={}", borrowId, borrow.getUserId(), isOverdue);
    }

    /**
     * 发送到期提醒（剩余 N 天）。
     * 步骤：查 DB → 遍历 → 去重检查 → 发送 OVERDUE_REMINDER
     */
    private void sendDueSoonReminders(int days) {
        List<BorrowPO> borrows = borrowMapper.selectDueSoon(days);
        if (borrows.isEmpty()) {
            return;
        }
        log.info("action=borrow_notification type=due_soon days={} count={}", days, borrows.size());
        for (BorrowPO borrow : borrows) {
            if (messageRecordMapper.existsByBizIdAndBizType(borrow.getId(), BizType.OVERDUE_REMINDER.getValue())) {
                continue;
            }
            Map<String, String> params = buildDueSoonParams(borrow, days);
            messageService.send(borrow.getUserId(), BizType.OVERDUE_REMINDER, borrow.getId(), params);
        }
    }

    /**
     * 发送逾期通知。
     * 步骤：查 DB → 遍历 → 去重检查 → 发送 OVERDUE
     */
    private void sendOverdueNotifications() {
        List<BorrowPO> borrows = borrowMapper.selectOverdue();
        if (borrows.isEmpty()) {
            return;
        }
        log.info("action=borrow_notification type=overdue count={}", borrows.size());
        for (BorrowPO borrow : borrows) {
            if (messageRecordMapper.existsByBizIdAndBizType(borrow.getId(), BizType.OVERDUE.getValue())) {
                continue;
            }
            Map<String, String> params = buildOverdueParams(borrow);
            messageService.send(borrow.getUserId(), BizType.OVERDUE, borrow.getId(), params);
        }
    }

    /** 构建到期提醒的模板变量：bookName, dueDate, daysLeft */
    private Map<String, String> buildDueSoonParams(BorrowPO borrow, int days) {
        Map<String, String> params = new HashMap<>();
        params.put("bookName", borrow.getBookName());
        params.put("dueDate", formatDate(borrow.getReturnTime()));
        params.put("daysLeft", String.valueOf(days));
        return params;
    }

    /** 构建逾期通知的模板变量：bookName, dueDate, overdueDays */
    private Map<String, String> buildOverdueParams(BorrowPO borrow) {
        long overdueDays = (System.currentTimeMillis() - borrow.getReturnTime().getTime()) / (1000 * 60 * 60 * 24);
        Map<String, String> params = new HashMap<>();
        params.put("bookName", borrow.getBookName());
        params.put("dueDate", formatDate(borrow.getReturnTime()));
        params.put("overdueDays", String.valueOf(Math.max(overdueDays, 1)));
        return params;
    }

    /** Date → yyyy-MM-dd 格式字符串 */
    private String formatDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT);
    }
}
