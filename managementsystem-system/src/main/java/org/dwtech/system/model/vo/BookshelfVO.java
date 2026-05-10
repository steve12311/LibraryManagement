package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 书架视图对象
 *
 * @author steve12311
 * @since 2026-05-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BookshelfVO extends BaseVO {

    private Long id;

    private Long floorId;

    private String shelfNo;

    private String name;

    private BigDecimal x;

    private BigDecimal y;

    private BigDecimal width;

    private BigDecimal height;

    private BigDecimal angle;

    private Integer capacity;

    private Integer usedStock;

    private Integer remainingCapacity;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
