package org.dwtech.system.service;

/**
 * 借阅通知服务，负责到期提醒和逾期通知。
 */
public interface BorrowNotificationService {

    /**
     * 扫描并发送所有到期/逾期通知（定时任务调用，带去重）。
     * 依次处理：剩余 3 天 → 剩余 1 天 → 已逾期。
     */
    void sendScheduledNotifications();

    /**
     * 管理员手动发送提醒（无视去重，直接发送）
     *
     * @param borrowId 借阅记录 ID
     */
    void sendReminder(String borrowId);
}
