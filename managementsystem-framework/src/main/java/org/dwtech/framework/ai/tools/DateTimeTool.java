package org.dwtech.framework.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
/**
 * DateTimeTool
 *
 * @author steve12311
 * @since 2026-02-22
 */

public class DateTimeTool {
    /**
     * 用途：获取 current date time 信息。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    @Tool(description = "获取当前时间")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}
