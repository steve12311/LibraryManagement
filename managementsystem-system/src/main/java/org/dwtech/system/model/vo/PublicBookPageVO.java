package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;

import java.math.BigDecimal;
import java.util.Date;

/**
 * PublicBookPageVO
 *
 * @author steve12311
 * @since 2026-03-23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PublicBookPageVO extends BaseVO {
    private String coverUrl;
    private String name;
    private String isbn;
    private Boolean available;
    private String intro;
    private String categoryName;
    private String publishName;
    private Date publishTime;
    private BigDecimal price;
    private String author;
}
