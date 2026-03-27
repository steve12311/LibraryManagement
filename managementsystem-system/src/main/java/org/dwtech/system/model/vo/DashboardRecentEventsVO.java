package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 大屏最近事件集合
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardRecentEventsVO {
    private DashboardListVO<DashboardRecentBorrowVO> borrows;
    private DashboardListVO<DashboardRecentOperLogVO> operLogs;
    private DashboardListVO<DashboardRecentAuthLogVO> authFailures;
}
