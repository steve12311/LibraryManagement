package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("lib_reservation")
public class ReservationPO {
    @TableId
    private String id;
    private String isbn;
    private String bookName;
    private Long userId;
    private Integer status;
    private Date pickupDeadline;
    private String borrowId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
