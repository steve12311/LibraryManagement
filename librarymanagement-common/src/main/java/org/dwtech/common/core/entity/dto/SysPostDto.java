package org.dwtech.common.core.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dwtech.common.valid.SysAddPostGroup;
import org.dwtech.common.valid.SysEditPostGroup;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SysPostDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(groups = SysEditPostGroup.class, message = "岗位Id不能为空")
    private Long postId;
    @NotBlank(groups = SysAddPostGroup.class, message = "岗位代码不能为空")
    private String postCode;
    @NotBlank(groups = SysAddPostGroup.class, message = "岗位名称不能为空")
    private String postName;
    private Integer postSort;
    private String status;
}
