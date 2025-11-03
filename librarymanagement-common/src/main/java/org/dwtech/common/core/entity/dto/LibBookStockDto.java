package org.dwtech.common.core.entity.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class LibBookStockDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long bookId;
    private String isbn;
    private BigDecimal price;
    private String bookName;
    private String bookImage;
    private String author;
    private Long publishId;
    private Date publishTime;
    private Long categoryId;
    private Long stockNumber;
    private Long currentNumber;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;
}
