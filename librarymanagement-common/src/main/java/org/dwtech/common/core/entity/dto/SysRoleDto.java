package org.dwtech.common.core.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dwtech.common.valid.AddGroup;
import org.dwtech.common.valid.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysRoleDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(groups = EditGroup.class, message = "角色ID不能为空")
    private Long roleId;
    @NotBlank(groups = AddGroup.class, message = "角色名称不能为空")
    private String roleName;
    @NotBlank(groups = AddGroup.class, message = "权限标识不能为空")
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private Date createTime;
}
