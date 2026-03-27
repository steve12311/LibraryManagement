package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.DashboardMetricHourPO;
import org.dwtech.system.model.vo.DashboardTrendPointVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小时汇总指标 Mapper
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Mapper
public interface DashboardMetricHourMapper extends BaseMapper<DashboardMetricHourPO> {
    void upsertMetric(DashboardMetricHourPO metric);

    List<DashboardTrendPointVO> selectTrend(@Param("startHour") LocalDateTime startHour);
}
