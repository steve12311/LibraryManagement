package org.dwtech.system.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReservationStatus {
    PENDING(0, "等待中"),
    READY(1, "可取书"),
    FULFILLED(2, "已完成"),
    EXPIRED(3, "已过期"),
    CANCELLED(4, "已取消");

    private final int value;
    private final String label;

    public static ReservationStatus fromValue(int value) {
        for (ReservationStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new IllegalArgumentException("Invalid reservation status: " + value);
    }
}
