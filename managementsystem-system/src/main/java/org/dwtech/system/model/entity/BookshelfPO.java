package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseEntity;

import java.math.BigDecimal;

/**
 * 图书馆书架实体
 *
 * @author steve12311
 * @since 2026-05-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lib_bookshelf")
public class BookshelfPO extends BaseEntity {

    private Long floorId;

    private String shelfNo;

    private String name;

    private BigDecimal x;

    private BigDecimal y;

    private BigDecimal width;

    private BigDecimal height;

    private BigDecimal angle;

    private Integer capacity;

    private Integer status;

    private String remark;
}
