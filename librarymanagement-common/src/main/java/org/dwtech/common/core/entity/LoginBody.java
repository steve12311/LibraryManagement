package org.dwtech.common.core.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dwtech.common.constant.UserConstants;

@Data
public class LoginBody {
    @NotBlank
    @Size(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH)
    private String username;
    @NotBlank
    @Size(min = UserConstants.PASSWORD_MIN_LENGTH, max = UserConstants.PASSWORD_MAX_LENGTH)
    private String password;
    @NotBlank
    @Size(min = 4, max = 4)
    private String code;
    @NotBlank
    private String uuid;
}
