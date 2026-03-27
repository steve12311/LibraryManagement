package org.dwtech.system.model.query;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.dwtech.common.annontation.ValidField;

/**
 * 大屏趋势查询参数
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardTrendQuery {

    @ValidField(allowedValues = {"day", "hour"}, message = "趋势模式不合法")
    private String mode = "day";

    private Integer days = 7;

    private Integer hours;

    @AssertTrue(message = "按天模式仅支持 7 天或 30 天")
    public boolean isDayWindowValid() {
        if (!"day".equals(mode)) {
            return true;
        }
        return days != null && (days == 7 || days == 30);
    }

    @AssertTrue(message = "按小时模式仅支持 24 小时")
    public boolean isHourWindowValid() {
        if (!"hour".equals(mode)) {
            return true;
        }
        return hours != null && hours == 24;
    }
}
