package org.dwtech.common.core.entity.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dwtech.common.valid.LibAddCategoryGroup;
import org.dwtech.common.valid.LibEditCategoryGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class LibCategoryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(groups = LibEditCategoryGroup.class, message = "分类Id不能为空")
    private Long categoryId;
    @NotBlank(groups = LibAddCategoryGroup.class, message = "分类代码不能为空")
    private String code;
    @NotNull(groups = LibAddCategoryGroup.class, message = "父分类Id不能为空")
    private Long parentId;
    @NotBlank(groups = LibAddCategoryGroup.class, message = "祖父Id列表不能为空")
    private String ancestors;
    @NotBlank(groups = LibAddCategoryGroup.class, message = "分类名称不能为空")
    private String categoryName;
    private Date createTime;
    private List<LibCategoryDto> children;
}
