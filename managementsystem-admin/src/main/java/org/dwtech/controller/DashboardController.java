package org.dwtech.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.query.DashboardRecentEventsQuery;
import org.dwtech.system.model.query.DashboardTrendQuery;
import org.dwtech.system.model.vo.*;
import org.dwtech.system.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据大屏接口
 *
 * @author steve12311
 * @since 2026-03-27
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * 获取数据大屏概览数据。
     * 包含总藏书量、总借阅次数、当前借出数、逾期未还数等关键指标。
     */
    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<DashboardOverviewVO> getOverview() {
        return Result.success(dashboardService.getOverview());
    }

    /**
     * 获取借阅趋势数据。
     * 按日/周/月维度返回借阅量变化曲线，用于数据大屏趋势图展示。
     *
     * @param queryParams 趋势查询参数，包含时间范围和聚合粒度
     */
    @GetMapping("/trends")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<List<DashboardTrendPointVO>> getTrends(@Valid DashboardTrendQuery queryParams) {
        return Result.success(dashboardService.getTrends(queryParams));
    }

    /**
     * 获取排行数据。
     * 包含热门图书排行、热门分类排行和最活跃读者排行，用于数据大屏展示。
     */
    @GetMapping("/rankings")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<DashboardRankingsVO> getRankings() {
        return Result.success(dashboardService.getRankings());
    }

    /**
     * 获取最近借阅动态。
     * 返回最新的借阅/归还事件列表，用于数据大屏滚动展示实时动态。
     *
     * @param queryParams 最近事件查询参数，包含分页和时间范围
     */
    @GetMapping("/recent-events")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<DashboardRecentEventsVO> getRecentEvents(@Valid DashboardRecentEventsQuery queryParams) {
        return Result.success(dashboardService.getRecentEvents(queryParams));
    }
}
