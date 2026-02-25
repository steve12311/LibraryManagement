package org.dwtech.common.enmus;

import lombok.Getter;

/**
 * 数据权限枚举
 *
 * @author steve12311
* @since 2025-11-18
 */
/**
 * DataScopeEnum
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Getter
public enum DataScopeEnum implements IBaseEnum<Integer> {

    /**
     * value 越小，数据权限范围越大
     */
    ALL(1, "所有数据"),
    DEPT_AND_SUB(2, "部门及子部门数据"),
    DEPT(3, "本部门数据"),
    SELF(4, "本人数据");

    private final Integer value;

    private final String label;

    /**
     * 用途：创建 DataScopeEnum 实例。
     * 
     * @param value value
     * @param label label
     * 返回：无。
     */
    DataScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
