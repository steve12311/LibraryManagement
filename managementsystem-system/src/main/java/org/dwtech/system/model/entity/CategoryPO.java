package org.dwtech.system.model.entity;

import org.dwtech.common.base.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lib_category")
public class CategoryPO extends BaseEntity {

    private Long parentId;

    private String treePath;

    private String name;

    private String type;

    private Integer visible;

    private Integer sort;
}
