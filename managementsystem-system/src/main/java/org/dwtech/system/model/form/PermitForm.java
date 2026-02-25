package org.dwtech.system.model.form;

import lombok.Data;
/**
 * PermitForm
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
public class PermitForm {
    private Long id;
    private Long parentId;
    private String label;
    private String value;
}
