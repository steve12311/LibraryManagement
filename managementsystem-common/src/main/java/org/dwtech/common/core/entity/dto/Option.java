package org.dwtech.common.core.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Option<T> {
    private T value;

    private String label;

    private String tag;

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
}
