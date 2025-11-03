package org.dwtech.common.core.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dwtech.common.valid.AddGroup;
import org.dwtech.common.valid.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class SysDeptDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(groups = EditGroup.class, message = "部门Id不能为空")
    protected Long deptId;
    @NotNull(groups = AddGroup.class, message = "父部门Id不能为空")
    protected Long parentId;
    @NotBlank(groups = AddGroup.class, message = "祖父Id列表不能为空")
    protected String ancestors;
    @NotBlank(groups = AddGroup.class, message = "部门名称不能为空")
    protected String deptName;
    protected Integer orderNum;
    @NotBlank(groups = AddGroup.class, message = "部门领导不能为空")
    protected String leader;
    protected String phone;
    protected String email;
    protected String status;
    protected Date createTime;
    protected List<SysDeptDto> children;
}
