package org.dwtech.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
/**
 * StockForm
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Data
public class StockForm {
    @NotBlank(message = "ISBN不能为空")
    private String isbn;
    private String cover;
    private String name;
    private String intro;
    private String author;
    private Long pressId;
    private Date publishTime;
    private Long categoryId;
    private BigDecimal price;
    private Long shelfId;
    private String shelfNo;
    @NotNull(message = "库存不能为空")
    @PositiveOrZero(message = "库存必须为非负数")
    private Integer stock;
}
