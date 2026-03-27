package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.DashboardMetricDayPO;
import org.dwtech.system.model.vo.DashboardTrendPointVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 日汇总指标 Mapper
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Mapper
public interface DashboardMetricDayMapper extends BaseMapper<DashboardMetricDayPO> {
    void upsertMetric(DashboardMetricDayPO metric);

    List<DashboardTrendPointVO> selectTrend(@Param("startDate") LocalDate startDate);
}
