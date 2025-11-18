package org.dwtech.common.core.entity.form;

import lombok.Data;

@Data
public class PermitForm {
    private Long id;
    private Long parentId;
    private String label;
    private String value;
}
