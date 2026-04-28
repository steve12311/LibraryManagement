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
     * 构造状态枚举项。
     *
     * @param value 状态值
     * @param label 状态描述
     */
    StatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
