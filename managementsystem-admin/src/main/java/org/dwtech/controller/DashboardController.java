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

    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<DashboardOverviewVO> getOverview() {
        return Result.success(dashboardService.getOverview());
    }

    @GetMapping("/trends")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<List<DashboardTrendPointVO>> getTrends(@Valid DashboardTrendQuery queryParams) {
        return Result.success(dashboardService.getTrends(queryParams));
    }

    @GetMapping("/rankings")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<DashboardRankingsVO> getRankings() {
        return Result.success(dashboardService.getRankings());
    }

    @GetMapping("/recent-events")
    @PreAuthorize("@ss.hasPerm('dashboard:view')")
    public Result<DashboardRecentEventsVO> getRecentEvents(@Valid DashboardRecentEventsQuery queryParams) {
        return Result.success(dashboardService.getRecentEvents(queryParams));
    }
}
