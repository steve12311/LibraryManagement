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
public class LibCategoryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(groups = EditGroup.class, message = "分类Id不能为空")
    private Long categoryId;
    @NotBlank(groups = AddGroup.class, message = "分类代码不能为空")
    private String code;
    @NotNull(groups = AddGroup.class, message = "父分类Id不能为空")
    private Long parentId;
    @NotBlank(groups = AddGroup.class, message = "祖父Id列表不能为空")
    private String ancestors;
    @NotBlank(groups = AddGroup.class, message = "分类名称不能为空")
    private String categoryName;
    private Date createTime;
    private List<LibCategoryDto> children;
}
