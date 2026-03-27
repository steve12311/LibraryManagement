package org.dwtech.system.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 大屏排行数据
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardRankingsVO {
    private List<DashboardHotBookVO> hotBooks;
    private List<DashboardNamedCountVO> operationModules;
    private List<DashboardNamedCountVO> authFailureUsers;
    private List<DashboardNamedCountVO> authFailureIps;
}
