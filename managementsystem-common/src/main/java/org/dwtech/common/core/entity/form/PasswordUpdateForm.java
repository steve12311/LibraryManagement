package org.dwtech.common.core.entity.form;

import lombok.Data;

@Data
public class PasswordUpdateForm {
    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
