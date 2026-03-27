package org.dwtech.common.enmus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证日志事件类型
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Getter
@AllArgsConstructor
public enum AuthEventTypeEnum {
    LOGIN("LOGIN"),
    LOGOUT("LOGOUT"),
    REFRESH_TOKEN("REFRESH_TOKEN");

    private final String value;
}
