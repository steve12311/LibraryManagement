package org.dwtech.system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.service.BorrowNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowNotificationTask {

    private final BorrowNotificationService borrowNotificationService;

    /** 每天 08:00 触发，扫描并发送到期/逾期通知（去重） */
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendNotifications() {
        log.info("action=borrow_notification_task result=start");
        borrowNotificationService.sendScheduledNotifications();
        log.info("action=borrow_notification_task result=done");
    }
}
