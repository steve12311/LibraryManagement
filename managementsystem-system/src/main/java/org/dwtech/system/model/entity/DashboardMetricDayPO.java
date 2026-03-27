package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日级大屏指标汇总
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
@TableName("dashboard_metric_day")
public class DashboardMetricDayPO {
    @TableId
    private LocalDate statDate;
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
