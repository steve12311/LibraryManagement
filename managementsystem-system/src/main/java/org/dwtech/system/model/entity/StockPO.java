package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * StockPO
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Data
@TableName("lib_stock")
public class StockPO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId
    private String isbn;
    private Integer stock;
    private Integer currentStock;
    private Long shelfId;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
