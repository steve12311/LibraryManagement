package org.dwtech.system.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 大屏最近事件查询参数
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardRecentEventsQuery {

    @Min(value = 1, message = "借阅页码必须大于等于 1")
    private int borrowPageNum = 1;

    @Min(value = 1, message = "借阅每页条数必须大于等于 1")
    @Max(value = 20, message = "借阅每页条数不能超过 20")
    private int borrowPageSize = 10;

    @Min(value = 1, message = "操作日志页码必须大于等于 1")
    private int operPageNum = 1;

    @Min(value = 1, message = "操作日志每页条数必须大于等于 1")
    @Max(value = 20, message = "操作日志每页条数不能超过 20")
    private int operPageSize = 10;

    @Min(value = 1, message = "认证日志页码必须大于等于 1")
    private int authPageNum = 1;

    @Min(value = 1, message = "认证日志每页条数必须大于等于 1")
    @Max(value = 20, message = "认证日志每页条数不能超过 20")
    private int authPageSize = 10;
}
