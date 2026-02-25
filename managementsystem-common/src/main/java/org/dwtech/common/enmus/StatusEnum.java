package org.dwtech.common.enmus;

import lombok.Getter;

/**
 * 状态枚举
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Getter
public enum StatusEnum implements IBaseEnum<Integer> {

    ENABLE(1, "启用"),
    DISABLE (0, "禁用");

    private final Integer value;


    private final String label;

    /**
     * 用途：创建 StatusEnum 实例。
     * 
     * @param value value
     * @param label label
     * 返回：无。
     */
    StatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
