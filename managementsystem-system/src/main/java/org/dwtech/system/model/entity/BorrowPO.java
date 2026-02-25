package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
/**
 * BorrowPO
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
@TableName("lib_borrow")
public class BorrowPO {
    private String id;
    private String isbn;
    private String bookName;
    private Long userId;
    private Date returnTime;
    private Date realityReturnTime;
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
