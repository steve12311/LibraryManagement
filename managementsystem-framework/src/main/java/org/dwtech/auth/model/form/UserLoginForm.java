package org.dwtech.auth.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
/**
 * UserLoginForm
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Getter
@Setter
public class UserLoginForm {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;
}
