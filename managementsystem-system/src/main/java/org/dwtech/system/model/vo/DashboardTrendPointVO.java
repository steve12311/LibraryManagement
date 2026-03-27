package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 大屏趋势点
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardTrendPointVO {
    private String timeBucket;
    private String label;
    private Long bookTotal;
    private Long stockTotal;
    private Long availableStockTotal;
    private Long borrowingTotal;
    private Long returnedTotal;
    private Long overdueTotal;
    private Long enabledUserTotal;
    private Long loginSuccessCount;
    private Long loginFailureCount;
    private Long operSuccessCount;
    private Long operFailureCount;
}
