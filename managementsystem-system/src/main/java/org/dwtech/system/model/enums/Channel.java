package org.dwtech.system.model.enums;

import lombok.Getter;

@Getter
public enum Channel {
    /** 邮件渠道 */
    EMAIL("EMAIL"),
    /** 短信渠道 */
    SMS("SMS");

    private final String value;

    Channel(String value) {
        this.value = value;
    }
}
