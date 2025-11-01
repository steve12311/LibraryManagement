package org.dwtech.common.core.entity.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class LibCategoryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long categoryId;
    private String code;
    private String categoryName;
    private Date createTime;
    private List<LibCategoryVo> children;
}
