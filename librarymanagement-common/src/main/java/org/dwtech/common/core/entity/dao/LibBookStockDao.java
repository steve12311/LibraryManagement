package org.dwtech.common.core.entity.dao;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class LibBookStockDao implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    protected Long bookId;
    protected String isbn;
    protected BigDecimal price;
    protected String bookName;
    protected String bookImage;
    protected String author;
    protected Long publishId;
    protected String publishName;
    protected Date publishTime;
    protected Long categoryId;
    protected String categoryName;
    protected Long stockNumber;
    protected Long currentNumber;
    protected String createBy;
    protected Date createTime;
    protected String updateBy;
    protected Date updateTime;
    protected String remark;
}
