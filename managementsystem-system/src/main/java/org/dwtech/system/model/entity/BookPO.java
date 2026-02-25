package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("lib_book")
public class BookPO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId
    private String isbn;
    private String cover;
    private String name;
    private String intro;
    private String author;
    private Long pressId;
    private Date publishTime;
    private Long categoryId;
    private BigDecimal price;
}
