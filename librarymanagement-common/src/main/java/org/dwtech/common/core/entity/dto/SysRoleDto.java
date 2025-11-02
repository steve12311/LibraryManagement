package org.dwtech.common.core.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dwtech.common.valid.sys.SysAddRoleGroup;
import org.dwtech.common.valid.sys.SysEditRoleGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysRoleDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(groups = SysEditRoleGroup.class, message = "角色ID不能为空")
    private Long roleId;
    @NotBlank(groups = SysAddRoleGroup.class, message = "角色名称不能为空")
    private String roleName;
    @NotBlank(groups = SysAddRoleGroup.class, message = "权限标识不能为空")
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private Date createTime;
}
