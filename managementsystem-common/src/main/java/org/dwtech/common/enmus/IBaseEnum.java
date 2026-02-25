package org.dwtech.common.enmus;


import cn.hutool.core.util.ObjectUtil;

import java.util.EnumSet;
import java.util.Objects;

/**
 * 枚举通用接口
 *
 * @author steve12311
 * @since 2025-11-18
 */
public interface IBaseEnum<T> {

    /**
     * 用途：获取 value 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    T getValue();

    /**
     * 用途：获取 label 信息。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String getLabel();

    /**
     * 用途：获取 enum by value 信息。
     *
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @param clazz 枚举类型
     * @param <E> 枚举泛型类型
     * @return 对应的枚举实例，不存在时返回 null
     */
    static <E extends Enum<E> & IBaseEnum> E getEnumByValue(Object value, Class<E> clazz) {
        Objects.requireNonNull(value);
        EnumSet<E> allEnums = EnumSet.allOf(clazz); // 获取类型下的所有枚举
        E matchEnum = allEnums.stream()
                .filter(e -> ObjectUtil.equal(e.getValue(), value))
                .findFirst()
                .orElse(null);
        return matchEnum;
    }

    /**
     * 用途：获取 label by value 信息。
     *
     * 根据文本标签获取值
     *
     * @param value 枚举值
     * @param clazz 枚举类型
     * @param <E> 枚举泛型类型
     * @return 对应的文本标签，不存在时返回 null
     */
    static <E extends Enum<E> & IBaseEnum> String getLabelByValue(Object value, Class<E> clazz) {
        Objects.requireNonNull(value);
        EnumSet<E> allEnums = EnumSet.allOf(clazz); // 获取类型下的所有枚举
        E matchEnum = allEnums.stream()
                .filter(e -> ObjectUtil.equal(e.getValue(), value))
                .findFirst()
                .orElse(null);

        String label = null;
        if (matchEnum != null) {
            label = matchEnum.getLabel();
        }
        return label;
    }


    /**
     * 用途：获取 value by label 信息。
     *
     * 根据文本标签获取值
     *
     * @param label 文本标签
     * @param clazz 枚举类型
     * @param <E> 枚举泛型类型
     * @return 对应的枚举值，不存在时返回 null
     */
    static <E extends Enum<E> & IBaseEnum> Object getValueByLabel(String label, Class<E> clazz) {
        Objects.requireNonNull(label);
        EnumSet<E> allEnums = EnumSet.allOf(clazz); // 获取类型下的所有枚举
        String finalLabel = label;
        E matchEnum = allEnums.stream()
                .filter(e -> ObjectUtil.equal(e.getLabel(), finalLabel))
                .findFirst()
                .orElse(null);

        Object value = null;
        if (matchEnum != null) {
            value = matchEnum.getValue();
        }
        return value;
    }


}
