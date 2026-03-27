package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.mapper.DashboardMetricDayMapper;
import org.dwtech.system.mapper.DashboardMetricHourMapper;
import org.dwtech.system.mapper.DashboardStatsMapper;
import org.dwtech.system.model.bo.DashboardMetricSnapshotBO;
import org.dwtech.system.model.entity.DashboardMetricDayPO;
import org.dwtech.system.model.entity.DashboardMetricHourPO;
import org.dwtech.system.model.query.DashboardRecentEventsQuery;
import org.dwtech.system.model.query.DashboardTrendQuery;
import org.dwtech.system.model.vo.DashboardHotBookVO;
import org.dwtech.system.model.vo.DashboardNamedCountVO;
import org.dwtech.system.model.vo.DashboardOverviewVO;
import org.dwtech.system.model.vo.DashboardRecentAuthLogVO;
import org.dwtech.system.model.vo.DashboardRecentBorrowVO;
import org.dwtech.system.model.vo.DashboardRecentEventsVO;
import org.dwtech.system.model.vo.DashboardRecentOperLogVO;
import org.dwtech.system.model.vo.DashboardTrendPointVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardStatsMapper dashboardStatsMapper;

    @Mock
    private DashboardMetricDayMapper dashboardMetricDayMapper;

    @Mock
    private DashboardMetricHourMapper dashboardMetricHourMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void shouldMapOverviewSnapshot() {
        DashboardMetricSnapshotBO snapshot = new DashboardMetricSnapshotBO();
        snapshot.setBookTotal(10L);
        snapshot.setStockTotal(50L);
        snapshot.setAvailableStockTotal(30L);
        snapshot.setBorrowingTotal(8L);
        snapshot.setReturnedTotal(12L);
        snapshot.setOverdueTotal(2L);
        snapshot.setEnabledUserTotal(6L);
        snapshot.setLoginSuccessCount(9L);
        snapshot.setLoginFailureCount(1L);
        snapshot.setOperSuccessCount(7L);
        snapshot.setOperFailureCount(3L);
        when(dashboardStatsMapper.selectSnapshotBetween(any(), any())).thenReturn(snapshot);

        DashboardOverviewVO overview = dashboardService.getOverview();

        assertThat(overview.getBookTotal()).isEqualTo(10L);
        assertThat(overview.getStockTotal()).isEqualTo(50L);
        assertThat(overview.getAvailableStockTotal()).isEqualTo(30L);
        assertThat(overview.getBorrowingTotal()).isEqualTo(8L);
        assertThat(overview.getReturnedTotal()).isEqualTo(12L);
        assertThat(overview.getOverdueTotal()).isEqualTo(2L);
        assertThat(overview.getEnabledUserTotal()).isEqualTo(6L);
        assertThat(overview.getTodayLoginSuccessCount()).isEqualTo(9L);
        assertThat(overview.getTodayLoginFailureCount()).isEqualTo(1L);
        assertThat(overview.getTodayOperSuccessCount()).isEqualTo(7L);
        assertThat(overview.getTodayOperFailureCount()).isEqualTo(3L);
    }

    @Test
    void shouldSelectDayOrHourTrendByMode() {
        DashboardTrendPointVO dayPoint = new DashboardTrendPointVO();
        dayPoint.setTimeBucket("2026-03-27");
        DashboardTrendPointVO hourPoint = new DashboardTrendPointVO();
        hourPoint.setTimeBucket("2026-03-27 10:00:00");
        when(dashboardMetricDayMapper.selectTrend(any(LocalDate.class))).thenReturn(List.of(dayPoint));
        when(dashboardMetricHourMapper.selectTrend(any(LocalDateTime.class))).thenReturn(List.of(hourPoint));

        DashboardTrendQuery dayQuery = new DashboardTrendQuery();
        dayQuery.setMode("day");
        dayQuery.setDays(7);
        DashboardTrendQuery hourQuery = new DashboardTrendQuery();
        hourQuery.setMode("hour");
        hourQuery.setHours(24);

        assertThat(dashboardService.getTrends(dayQuery)).containsExactly(dayPoint);
        assertThat(dashboardService.getTrends(hourQuery)).containsExactly(hourPoint);
    }

    @Test
    void shouldReturnEmptyRankingListsAndNormalizeBookCovers() {
        DashboardHotBookVO hotBook = new DashboardHotBookVO();
        hotBook.setIsbn("9787300000001");
        hotBook.setCover("/123");
        when(dashboardStatsMapper.selectHotBorrowBooks(any(), anyLong())).thenReturn(List.of(hotBook));
        when(dashboardStatsMapper.selectOperModuleRanking(any(), anyLong())).thenReturn(null);
        when(dashboardStatsMapper.selectAuthFailureUsernameRanking(any(), anyLong())).thenReturn(null);
        when(dashboardStatsMapper.selectAuthFailureIpRanking(any(), anyLong())).thenReturn(null);

        var rankings = dashboardService.getRankings();

        assertThat(rankings.getHotBooks()).hasSize(1);
        assertThat(rankings.getHotBooks().get(0).getCover()).isEqualTo("/api/v1/files/123");
        assertThat(rankings.getOperationModules()).isEmpty();
        assertThat(rankings.getAuthFailureUsers()).isEmpty();
        assertThat(rankings.getAuthFailureIps()).isEmpty();
    }

    @Test
    void shouldNormalizeRecentEventCoverAndWrapPages() {
        DashboardRecentBorrowVO borrowVO = new DashboardRecentBorrowVO();
        borrowVO.setBorrowId("borrow-1");
        borrowVO.setCover("/42");
        borrowVO.setReturnTime(new Date());
        Page<DashboardRecentBorrowVO> borrowPage = new Page<>(1, 10);
        borrowPage.setRecords(List.of(borrowVO));
        borrowPage.setTotal(1);

        DashboardRecentOperLogVO operLogVO = new DashboardRecentOperLogVO();
        operLogVO.setLogId(1L);
        Page<DashboardRecentOperLogVO> operPage = new Page<>(1, 10);
        operPage.setRecords(List.of(operLogVO));
        operPage.setTotal(1);

        DashboardRecentAuthLogVO authLogVO = new DashboardRecentAuthLogVO();
        authLogVO.setLogId(2L);
        Page<DashboardRecentAuthLogVO> authPage = new Page<>(1, 10);
        authPage.setRecords(List.of(authLogVO));
        authPage.setTotal(1);

        when(dashboardStatsMapper.selectRecentBorrowPage(any())).thenReturn(borrowPage);
        when(dashboardStatsMapper.selectRecentOperLogPage(any())).thenReturn(operPage);
        when(dashboardStatsMapper.selectRecentAuthFailurePage(any())).thenReturn(authPage);

        DashboardRecentEventsQuery query = new DashboardRecentEventsQuery();
        DashboardRecentEventsVO recentEvents = dashboardService.getRecentEvents(query);

        assertThat(recentEvents.getBorrows().getTotal()).isEqualTo(1L);
        assertThat(recentEvents.getBorrows().getList().get(0).getCover()).isEqualTo("/api/v1/files/42");
        assertThat(recentEvents.getOperLogs().getTotal()).isEqualTo(1L);
        assertThat(recentEvents.getAuthFailures().getTotal()).isEqualTo(1L);
    }

    @Test
    void shouldRefreshCurrentMetricBucketsByUpsertingDayAndHour() {
        DashboardMetricSnapshotBO snapshot = new DashboardMetricSnapshotBO();
        snapshot.setBookTotal(11L);
        snapshot.setStockTotal(22L);
        snapshot.setAvailableStockTotal(20L);
        snapshot.setBorrowingTotal(5L);
        snapshot.setReturnedTotal(3L);
        snapshot.setOverdueTotal(1L);
        snapshot.setEnabledUserTotal(7L);
        snapshot.setLoginSuccessCount(6L);
        snapshot.setLoginFailureCount(2L);
        snapshot.setOperSuccessCount(9L);
        snapshot.setOperFailureCount(4L);
        when(dashboardStatsMapper.selectSnapshotBetween(any(), any())).thenReturn(snapshot);

        dashboardService.refreshCurrentMetricBuckets();

        ArgumentCaptor<DashboardMetricDayPO> dayCaptor = ArgumentCaptor.forClass(DashboardMetricDayPO.class);
        verify(dashboardMetricDayMapper).upsertMetric(dayCaptor.capture());
        assertThat(dayCaptor.getValue().getStatDate()).isNotNull();
        assertThat(dayCaptor.getValue().getBookTotal()).isEqualTo(11L);
        assertThat(dayCaptor.getValue().getOperFailureCount()).isEqualTo(4L);

        ArgumentCaptor<DashboardMetricHourPO> hourCaptor = ArgumentCaptor.forClass(DashboardMetricHourPO.class);
        verify(dashboardMetricHourMapper).upsertMetric(hourCaptor.capture());
        assertThat(hourCaptor.getValue().getStatHour()).isNotNull();
        assertThat(hourCaptor.getValue().getStockTotal()).isEqualTo(22L);
        assertThat(hourCaptor.getValue().getLoginSuccessCount()).isEqualTo(6L);
    }
}
