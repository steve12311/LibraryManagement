package org.dwtech.system.model.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 书架表单
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class BookshelfForm {

    private Long id;

    @NotNull(message = "楼层不能为空")
    private Long floorId;

    @NotBlank(message = "书架号不能为空")
    private String shelfNo;

    private String name;

    @NotNull(message = "横坐标不能为空")
    @PositiveOrZero(message = "横坐标必须为非负数")
    private BigDecimal x;

    @NotNull(message = "纵坐标不能为空")
    @PositiveOrZero(message = "纵坐标必须为非负数")
    private BigDecimal y;

    @NotNull(message = "书架宽度不能为空")
    @Positive(message = "书架宽度必须大于0")
    private BigDecimal width;

    @NotNull(message = "书架高度不能为空")
    @Positive(message = "书架高度必须大于0")
    private BigDecimal height;

    @NotNull(message = "旋转角度不能为空")
    @DecimalMin(value = "-360.0", message = "旋转角度不能小于-360")
    @DecimalMax(value = "360.0", message = "旋转角度不能大于360")
    private BigDecimal angle;

    @NotNull(message = "书架容量不能为空")
    @Positive(message = "书架容量必须大于0")
    private Integer capacity;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
