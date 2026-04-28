package org.dwtech.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * Option
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
@NoArgsConstructor
public class Option<T> {
    private T value;

    private String label;

    private String tag;

    private Avatar avatar;

    private List<Option<T>> children;

    /**
     * 构造下拉选项。
     *
     * @param value 选项值
     * @param label 选项标签
     */
    public Option(T value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 构造带子级的下拉选项。
     *
     * @param value    选项值
     * @param label    选项标签
     * @param children 子选项列表
     */
    public Option(T value, String label, List<Option<T>> children) {
        this.value = value;
        this.label = label;
        this.children = children;
    }

    /**
     * 构造带标签的下拉选项。
     *
     * @param value 选项值
     * @param label 选项标签
     * @param tag   选项标签类型
     */
    public Option(T value, String label, String tag) {
        this.value = value;
        this.label = label;
        this.tag = tag;
    }

    /**
     * 构造带头像的下拉选项。
     *
     * @param value  选项值
     * @param label  选项标签
     * @param avatar 头像信息
     */
    public Option(T value, String label, Avatar avatar) {
        this.value = value;
        this.label = label;
        this.avatar = avatar;
    }
}
