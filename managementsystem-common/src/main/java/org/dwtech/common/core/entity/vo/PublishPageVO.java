package org.dwtech.common.core.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class PublishPageVO extends BaseVO {
    private Long publishId;
    private String publishName;
    private String address;
    private String addressCode;
    private String phonenumber;
    private Date createTime;
}
