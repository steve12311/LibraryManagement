package org.dwtech.system.model.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class StockBO {
    private String isbn;
    private String cover;
    private String name;
    private String intro;
    private String author;
    private Long pressId;
    private String pressName;
    private Date publishTime;
    private Long categoryId;
    private String categoryName;
    private Integer stock;
    private Integer currentStock;
    private BigDecimal price;
    private Date createTime;
    private Date updateTime;
}
