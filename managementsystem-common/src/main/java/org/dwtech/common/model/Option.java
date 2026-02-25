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
     * 用途：创建 Option 实例。
     * 
     * @param value value
     * @param label label
     * 返回：无。
     */
    public Option(T value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 用途：创建 Option 实例。
     * 
     * @param value value
     * @param label label
     * @param children children
     * 返回：无。
     */
    public Option(T value, String label, List<Option<T>> children) {
        this.value = value;
        this.label = label;
        this.children = children;
    }

    /**
     * 用途：创建 Option 实例。
     * 
     * @param value value
     * @param label label
     * @param tag tag
     * 返回：无。
     */
    public Option(T value, String label, String tag) {
        this.value = value;
        this.label = label;
        this.tag = tag;
    }

    /**
     * 用途：创建 Option 实例。
     * 
     * @param value value
     * @param label label
     * @param avatar avatar
     * 返回：无。
     */
    public Option(T value, String label, Avatar avatar) {
        this.value = value;
        this.label = label;
        this.avatar = avatar;
    }
}
