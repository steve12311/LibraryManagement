package org.dwtech.system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.service.DashboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 大屏指标汇总调度任务
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardMetricScheduler {
    private final DashboardService dashboardService;

    /**
     * 每五分钟刷新一次当前小时和当前天的汇总桶。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void refreshCurrentMetricBuckets() {
        dashboardService.refreshCurrentMetricBuckets();
        log.info("数据大屏汇总桶刷新完成");
    }
}
