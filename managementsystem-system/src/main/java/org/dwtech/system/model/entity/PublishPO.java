package org.dwtech.system.model.entity;

import org.dwtech.common.base.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lib_publish")
public class PublishPO extends BaseEntity {
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
