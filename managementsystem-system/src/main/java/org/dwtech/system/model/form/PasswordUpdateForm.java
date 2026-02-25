package org.dwtech.system.model.form;

import lombok.Data;
/**
 * PasswordUpdateForm
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
public class PasswordUpdateForm {
    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
