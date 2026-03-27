package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 大屏总览数据
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardOverviewVO {
    private Long bookTotal;
    private Long stockTotal;
    private Long availableStockTotal;
    private Long borrowingTotal;
    private Long returnedTotal;
    private Long overdueTotal;
    private Long enabledUserTotal;
    private Long todayLoginSuccessCount;
    private Long todayLoginFailureCount;
    private Long todayOperSuccessCount;
    private Long todayOperFailureCount;
}
