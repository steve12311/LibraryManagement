package org.dwtech.common.core.entity.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class RoleForm {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    private Integer sort;

    @Range(max = 1, min = 0, message = "角色状态不正确")
    private Integer status;

    private Integer dataScope;

}
