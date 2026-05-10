package org.dwtech.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 图书馆楼层表单
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class LibraryFloorForm {

    private Long id;

    @NotNull(message = "楼层不能为空")
    @Positive(message = "楼层必须为正数")
    private Integer floorNo;

    @NotBlank(message = "楼层名称不能为空")
    private String name;

    private String outlineJson;

    @PositiveOrZero(message = "排序值必须为非负数")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
