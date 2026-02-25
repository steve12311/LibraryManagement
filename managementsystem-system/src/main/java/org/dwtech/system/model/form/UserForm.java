package org.dwtech.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;
/**
 * UserForm
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
public class UserForm {
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;


    @Pattern(regexp = "^$|^1(3\\d|4[5-9]|5[0-35-9]|6[2567]|7[0-8]|8\\d|9[0-35-9])\\d{8}$", message = "手机号码格式不正确")
    private String mobile;

    private Integer gender;

    private String avatar;

    private String email;

    @Range(min = 0, max = 1, message = "用户状态不正确")
    private Integer status;

    private Long deptId;

    @NotEmpty(message = "用户角色不能为空")
    private List<Long> roleIds;

    private String openId;
}
