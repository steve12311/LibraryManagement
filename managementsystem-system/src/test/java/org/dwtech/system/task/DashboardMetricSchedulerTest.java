package org.dwtech.system.task;

import org.dwtech.system.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardMetricSchedulerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardMetricScheduler dashboardMetricScheduler;

    @Test
    void shouldDelegateScheduledRefreshToDashboardService() {
        dashboardMetricScheduler.refreshCurrentMetricBuckets();

        verify(dashboardService).refreshCurrentMetricBuckets();
    }
}
