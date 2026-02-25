package org.dwtech.system.model.form;

import lombok.Data;

@Data
public class PasswordUpdateForm {
    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
