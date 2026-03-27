package org.dwtech.system.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大屏最近认证失败日志
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardRecentAuthLogVO {
    private Long logId;
    private String eventType;
    private String username;
    private String clientIp;
    private String resultCode;
    private String failureSummary;
    private LocalDateTime createTime;
}
