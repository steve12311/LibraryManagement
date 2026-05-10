package org.dwtech.system.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 楼层轮廓表单
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class FloorOutlineForm {

    @NotBlank(message = "楼层轮廓不能为空")
    private String outlineJson;
}
