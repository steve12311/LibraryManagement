package org.dwtech.system.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 大屏最近借阅事件
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardRecentBorrowVO {
    private String borrowId;
    private String isbn;
    private String cover;
    private String bookName;
    private String username;
    private Date returnTime;
    private Integer status;
    private LocalDateTime createTime;
}
