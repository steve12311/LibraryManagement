package org.dwtech.common.core.entity.dto;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class LibCategoryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long categoryId;
    private String code;
    private Long parentId;
    private String ancestors;
    private String categoryName;
    private Date createTime;
    private List<LibCategoryDto> children;
}
