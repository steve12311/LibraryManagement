package org.dwtech.common.core.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lib_publish")
public class PublishPO extends BasePO {
    private String name;
    private String country;
    private String province;
    private String city;
    private String area;
    private String areaDetail;
    private String telephone;
    private String postalCode;
    private String email;
    private Integer isDeleted;
}
