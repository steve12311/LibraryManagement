package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class StockPageVO extends BaseVO {
    private String isbn;
    private String bookImage;
    private String name;
    private String intro;
    private String author;
    private String publishName;
    private Date publishTime;
    private String categoryName;
    private Integer stockNumber;
    private Integer currentNumber;
    private BigDecimal price;
    private Date createTime;
}
