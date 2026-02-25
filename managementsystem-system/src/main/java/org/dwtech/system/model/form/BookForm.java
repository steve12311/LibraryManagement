package org.dwtech.system.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BookForm {
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
}
