package org.dwtech.system.service;

import org.dwtech.system.model.query.DashboardRecentEventsQuery;
import org.dwtech.system.model.query.DashboardTrendQuery;
import org.dwtech.system.model.vo.*;

import java.util.List;

/**
 * 大屏统计服务
 *
 * @author steve12311
 * @since 2026-03-27
 */
public interface DashboardService {

    DashboardOverviewVO getOverview();

    List<DashboardTrendPointVO> getTrends(DashboardTrendQuery queryParams);

    DashboardRankingsVO getRankings();

    DashboardRecentEventsVO getRecentEvents(DashboardRecentEventsQuery queryParams);

    void refreshCurrentMetricBuckets();
}
