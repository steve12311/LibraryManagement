package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dwtech.system.mapper.DashboardMetricDayMapper;
import org.dwtech.system.mapper.DashboardMetricHourMapper;
import org.dwtech.system.mapper.DashboardStatsMapper;
import org.dwtech.system.model.bo.DashboardMetricSnapshotBO;
import org.dwtech.system.model.entity.DashboardMetricDayPO;
import org.dwtech.system.model.entity.DashboardMetricHourPO;
import org.dwtech.system.model.query.DashboardRecentEventsQuery;
import org.dwtech.system.model.query.DashboardTrendQuery;
import org.dwtech.system.model.vo.*;
import org.dwtech.system.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

/**
 * 大屏统计服务实现
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final int DEFAULT_RANKING_LIMIT = 5;

    private final DashboardStatsMapper dashboardStatsMapper;
    private final DashboardMetricDayMapper dashboardMetricDayMapper;
    private final DashboardMetricHourMapper dashboardMetricHourMapper;

    @Override
    public DashboardOverviewVO getOverview() {
        LocalDateTime now = LocalDateTime.now();
        DashboardMetricSnapshotBO snapshot = defaultSnapshot(
                dashboardStatsMapper.selectSnapshotBetween(LocalDate.now().atStartOfDay(), now)
        );
        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setBookTotal(valueOrZero(snapshot.getBookTotal()));
        overview.setStockTotal(valueOrZero(snapshot.getStockTotal()));
        overview.setAvailableStockTotal(valueOrZero(snapshot.getAvailableStockTotal()));
        overview.setBorrowingTotal(valueOrZero(snapshot.getBorrowingTotal()));
        overview.setReturnedTotal(valueOrZero(snapshot.getReturnedTotal()));
        overview.setOverdueTotal(valueOrZero(snapshot.getOverdueTotal()));
        overview.setEnabledUserTotal(valueOrZero(snapshot.getEnabledUserTotal()));
        overview.setTodayLoginSuccessCount(valueOrZero(snapshot.getLoginSuccessCount()));
        overview.setTodayLoginFailureCount(valueOrZero(snapshot.getLoginFailureCount()));
        overview.setTodayOperSuccessCount(valueOrZero(snapshot.getOperSuccessCount()));
        overview.setTodayOperFailureCount(valueOrZero(snapshot.getOperFailureCount()));
        return overview;
    }

    @Override
    public List<DashboardTrendPointVO> getTrends(DashboardTrendQuery queryParams) {
        if ("hour".equals(queryParams.getMode())) {
            LocalDateTime startHour = LocalDateTime.now()
                    .truncatedTo(ChronoUnit.HOURS)
                    .minusHours(queryParams.getHours() - 1L);
            return dashboardMetricHourMapper.selectTrend(startHour);
        }
        LocalDate startDate = LocalDate.now().minusDays(queryParams.getDays() - 1L);
        return dashboardMetricDayMapper.selectTrend(startDate);
    }

    @Override
    public DashboardRankingsVO getRankings() {
        LocalDateTime now = LocalDateTime.now();
        DashboardRankingsVO rankings = new DashboardRankingsVO();
        rankings.setHotBooks(normalizeBookCovers(
                dashboardStatsMapper.selectHotBorrowBooks(now.minusDays(30), DEFAULT_RANKING_LIMIT)
        ));
        rankings.setOperationModules(emptyIfNull(
                dashboardStatsMapper.selectOperModuleRanking(now.minusHours(24), DEFAULT_RANKING_LIMIT)
        ));
        rankings.setAuthFailureUsers(emptyIfNull(
                dashboardStatsMapper.selectAuthFailureUsernameRanking(now.minusHours(24), DEFAULT_RANKING_LIMIT)
        ));
        rankings.setAuthFailureIps(emptyIfNull(
                dashboardStatsMapper.selectAuthFailureIpRanking(now.minusHours(24), DEFAULT_RANKING_LIMIT)
        ));
        return rankings;
    }

    @Override
    public DashboardRecentEventsVO getRecentEvents(DashboardRecentEventsQuery queryParams) {
        DashboardRecentEventsVO recentEvents = new DashboardRecentEventsVO();

        Page<DashboardRecentBorrowVO> borrowPage = dashboardStatsMapper.selectRecentBorrowPage(
                new Page<>(queryParams.getBorrowPageNum(), queryParams.getBorrowPageSize())
        );
        borrowPage.getRecords().forEach(item -> item.setCover(normalizeCoverUrl(item.getCover())));
        recentEvents.setBorrows(toListVo(borrowPage));

        Page<DashboardRecentOperLogVO> operLogPage = dashboardStatsMapper.selectRecentOperLogPage(
                new Page<>(queryParams.getOperPageNum(), queryParams.getOperPageSize())
        );
        recentEvents.setOperLogs(toListVo(operLogPage));

        Page<DashboardRecentAuthLogVO> authLogPage = dashboardStatsMapper.selectRecentAuthFailurePage(
                new Page<>(queryParams.getAuthPageNum(), queryParams.getAuthPageSize())
        );
        recentEvents.setAuthFailures(toListVo(authLogPage));
        return recentEvents;
    }

    @Override
    public void refreshCurrentMetricBuckets() {
        LocalDateTime now = LocalDateTime.now();
        upsertDayMetric(now.toLocalDate(), now);
        upsertHourMetric(now.truncatedTo(ChronoUnit.HOURS), now);
    }

    private void upsertDayMetric(LocalDate statDate, LocalDateTime endTime) {
        DashboardMetricSnapshotBO snapshot = defaultSnapshot(
                dashboardStatsMapper.selectSnapshotBetween(statDate.atStartOfDay(), endTime)
        );
        DashboardMetricDayPO metric = new DashboardMetricDayPO();
        metric.setStatDate(statDate);
        applySnapshot(metric, snapshot);
        dashboardMetricDayMapper.upsertMetric(metric);
    }

    private void upsertHourMetric(LocalDateTime statHour, LocalDateTime endTime) {
        DashboardMetricSnapshotBO snapshot = defaultSnapshot(
                dashboardStatsMapper.selectSnapshotBetween(statHour, endTime)
        );
        DashboardMetricHourPO metric = new DashboardMetricHourPO();
        metric.setStatHour(statHour);
        applySnapshot(metric, snapshot);
        dashboardMetricHourMapper.upsertMetric(metric);
    }

    private void applySnapshot(DashboardMetricDayPO metric, DashboardMetricSnapshotBO snapshot) {
        applySnapshot(new MetricSnapshotSetters(
                metric::setBookTotal,
                metric::setStockTotal,
                metric::setAvailableStockTotal,
                metric::setBorrowingTotal,
                metric::setReturnedTotal,
                metric::setOverdueTotal,
                metric::setEnabledUserTotal,
                metric::setLoginSuccessCount,
                metric::setLoginFailureCount,
                metric::setOperSuccessCount,
                metric::setOperFailureCount
        ), snapshot);
    }

    private void applySnapshot(DashboardMetricHourPO metric, DashboardMetricSnapshotBO snapshot) {
        applySnapshot(new MetricSnapshotSetters(
                metric::setBookTotal,
                metric::setStockTotal,
                metric::setAvailableStockTotal,
                metric::setBorrowingTotal,
                metric::setReturnedTotal,
                metric::setOverdueTotal,
                metric::setEnabledUserTotal,
                metric::setLoginSuccessCount,
                metric::setLoginFailureCount,
                metric::setOperSuccessCount,
                metric::setOperFailureCount
        ), snapshot);
    }

    private void applySnapshot(MetricSnapshotSetters setters, DashboardMetricSnapshotBO snapshot) {
        setters.bookTotal().accept(valueOrZero(snapshot.getBookTotal()));
        setters.stockTotal().accept(valueOrZero(snapshot.getStockTotal()));
        setters.availableStockTotal().accept(valueOrZero(snapshot.getAvailableStockTotal()));
        setters.borrowingTotal().accept(valueOrZero(snapshot.getBorrowingTotal()));
        setters.returnedTotal().accept(valueOrZero(snapshot.getReturnedTotal()));
        setters.overdueTotal().accept(valueOrZero(snapshot.getOverdueTotal()));
        setters.enabledUserTotal().accept(valueOrZero(snapshot.getEnabledUserTotal()));
        setters.loginSuccessCount().accept(valueOrZero(snapshot.getLoginSuccessCount()));
        setters.loginFailureCount().accept(valueOrZero(snapshot.getLoginFailureCount()));
        setters.operSuccessCount().accept(valueOrZero(snapshot.getOperSuccessCount()));
        setters.operFailureCount().accept(valueOrZero(snapshot.getOperFailureCount()));
    }

    private DashboardMetricSnapshotBO defaultSnapshot(DashboardMetricSnapshotBO snapshot) {
        return snapshot == null ? new DashboardMetricSnapshotBO() : snapshot;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private <T> DashboardListVO<T> toListVo(Page<T> page) {
        DashboardListVO<T> listVO = new DashboardListVO<>();
        listVO.setList(page.getRecords());
        listVO.setTotal(page.getTotal());
        return listVO;
    }

    private List<DashboardHotBookVO> normalizeBookCovers(List<DashboardHotBookVO> hotBooks) {
        if (hotBooks == null) {
            return List.of();
        }
        hotBooks.forEach(item -> item.setCover(normalizeCoverUrl(item.getCover())));
        return hotBooks;
    }

    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String normalizeCoverUrl(String cover) {
        if (cover == null || cover.isBlank()) {
            return cover;
        }
        if (cover.startsWith("/api/v1/files/")) {
            return cover;
        }
        if (cover.startsWith("/")) {
            return "/api/v1/files" + cover;
        }
        return cover;
    }

    private record MetricSnapshotSetters(
            Consumer<Long> bookTotal,
            Consumer<Long> stockTotal,
            Consumer<Long> availableStockTotal,
            Consumer<Long> borrowingTotal,
            Consumer<Long> returnedTotal,
            Consumer<Long> overdueTotal,
            Consumer<Long> enabledUserTotal,
            Consumer<Long> loginSuccessCount,
            Consumer<Long> loginFailureCount,
            Consumer<Long> operSuccessCount,
            Consumer<Long> operFailureCount
    ) {
    }
}
