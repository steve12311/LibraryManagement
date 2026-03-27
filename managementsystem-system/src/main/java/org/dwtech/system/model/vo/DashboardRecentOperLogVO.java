package org.dwtech.system.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大屏最近操作日志
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardRecentOperLogVO {
    private Long logId;
    private String module;
    private String action;
    private String operatorUsername;
    private Integer success;
    private String resultCode;
    private LocalDateTime createTime;
}
