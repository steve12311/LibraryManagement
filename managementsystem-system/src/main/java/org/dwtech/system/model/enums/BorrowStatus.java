package org.dwtech.system.model.enums;

import lombok.Getter;

/**
 * 借阅状态枚举，对应 SQL 中的 CASE 计算值
 *
 * <p>状态判定逻辑（基于 lib_borrow 表的两个时间字段）：
 * <ul>
 *   <li>{@link #RETURNED} — reality_return_time 不为空，图书已归还</li>
 *   <li>{@link #BORROWING} — 未归还且 return_time >= NOW()，正常借阅中</li>
 *   <li>{@link #OVERDUE}   — 未归还且 return_time &lt; NOW()，已超期</li>
 * </ul>
 *
 * @author steve12311
 * @since 2026-05-20
 */
@Getter
public enum BorrowStatus {

    /** 已归还 */
    RETURNED(0, "已归还"),
    /** 借阅中（未超期） */
    BORROWING(1, "借阅中"),
    /** 已逾期 */
    OVERDUE(2, "已逾期");

    private final int code;
    private final String description;

    BorrowStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
