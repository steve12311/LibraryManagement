package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.DashboardMetricSnapshotBO;
import org.dwtech.system.model.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 大屏统计查询 Mapper
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Mapper
public interface DashboardStatsMapper {

    DashboardMetricSnapshotBO selectSnapshotBetween(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    List<DashboardHotBookVO> selectHotBorrowBooks(@Param("startTime") LocalDateTime startTime,
                                                  @Param("limit") long limit);

    List<DashboardNamedCountVO> selectOperModuleRanking(@Param("startTime") LocalDateTime startTime,
                                                        @Param("limit") long limit);

    List<DashboardNamedCountVO> selectAuthFailureUsernameRanking(@Param("startTime") LocalDateTime startTime,
                                                                 @Param("limit") long limit);

    List<DashboardNamedCountVO> selectAuthFailureIpRanking(@Param("startTime") LocalDateTime startTime,
                                                           @Param("limit") long limit);

    Page<DashboardRecentBorrowVO> selectRecentBorrowPage(Page<DashboardRecentBorrowVO> page);

    Page<DashboardRecentOperLogVO> selectRecentOperLogPage(Page<DashboardRecentOperLogVO> page);

    Page<DashboardRecentAuthLogVO> selectRecentAuthFailurePage(Page<DashboardRecentAuthLogVO> page);
}
