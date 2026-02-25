package org.dwtech.system.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * PublishForm
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Data
public class PublishForm {
    private Long id;
    @NotBlank(message = "名称不能为空")
    private String name;
    private String country;
    private String province;
    private String city;
    private String area;
    private String areaDetail;
    private String postalCode;
    private String telephone;
    private String email;
}
