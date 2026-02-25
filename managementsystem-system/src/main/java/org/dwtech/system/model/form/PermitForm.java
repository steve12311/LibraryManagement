package org.dwtech.system.model.form;

import lombok.Data;

@Data
public class PermitForm {
    private Long id;
    private Long parentId;
    private String label;
    private String value;
}
