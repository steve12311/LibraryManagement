package org.dwtech.system.model.enums;

import lombok.Getter;

@Getter
public enum BizType {
    /** 预约到书通知 */
    RESERVATION_READY("RESERVATION_READY"),
    /** 预约即将到期通知 */
    RESERVATION_EXPIRING("RESERVATION_EXPIRING"),
    /** 逾期通知（已超期） */
    OVERDUE("OVERDUE"),
    /** 到期提醒（即将到期：3天/1天） */
    OVERDUE_REMINDER("OVERDUE_REMINDER"),
    /** 系统通知 */
    SYSTEM("SYSTEM");

    private final String value;

    BizType(String value) {
        this.value = value;
    }
}
