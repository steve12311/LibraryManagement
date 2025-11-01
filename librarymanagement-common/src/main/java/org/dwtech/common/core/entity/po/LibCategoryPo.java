package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LibCategoryPo extends BasePo {
    private Long categoryId;
    private String code;
    private Long parentId;
    private String ancestors;
    private String categoryName;
}
