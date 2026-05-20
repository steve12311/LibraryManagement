package org.dwtech.system.model.enums;

import lombok.Getter;

@Getter
public enum MessageStatus {
    /** 待发送（已入队，等待 Consumer 投递） */
    PENDING(0, "待发送"),
    /** 已发送（渠道投递成功） */
    SENT(1, "已发送"),
    /** 发送失败（重试次数耗尽或渠道不可用） */
    FAILED(2, "发送失败");

    private final int value;
    private final String label;

    MessageStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static MessageStatus fromValue(int value) {
        for (MessageStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown MessageStatus value: " + value);
    }
}
