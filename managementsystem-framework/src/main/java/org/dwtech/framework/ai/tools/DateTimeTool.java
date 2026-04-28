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
     * 获取当前日期时间，供 AI 模型使用。
     *
     * @return 当前日期时间的字符串表示
     */
    @Tool(description = "获取当前时间")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}
