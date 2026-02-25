package org.dwtech.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Option<T> {
    private T value;

    private String label;

    private String tag;

    private Avatar avatar;

    private List<Option<T>> children;

    public Option(T value, String label) {
        this.value = value;
        this.label = label;
    }

    public Option(T value, String label, List<Option<T>> children) {
        this.value = value;
        this.label = label;
        this.children = children;
    }

    public Option(T value, String label, String tag) {
        this.value = value;
        this.label = label;
        this.tag = tag;
    }

    public Option(T value, String label, Avatar avatar) {
        this.value = value;
        this.label = label;
        this.avatar = avatar;
    }
}
