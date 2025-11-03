package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class LibBookPo extends BasePo {
    protected Long bookId;
    protected String isbn;
    protected BigDecimal price;
    protected String bookName;
    protected String bookImage;
    protected String author;
    protected Long publishId;
    protected Long categoryId;
}
