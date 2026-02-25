package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryVO extends BaseVO {
    private Long categoryId;

    private String code;

    private Long parentId;

    private String treePath;

    private String categoryName;

    private List<CategoryVO> children;
}
