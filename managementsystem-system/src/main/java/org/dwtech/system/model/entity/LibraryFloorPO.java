package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseEntity;

/**
 * 图书馆楼层实体
 *
 * @author steve12311
 * @since 2026-05-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lib_floor")
public class LibraryFloorPO extends BaseEntity {

    private Integer floorNo;

    private String name;

    private String outlineJson;

    private Integer sort;

    private Integer status;
}
