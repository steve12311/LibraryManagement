package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LibPublishPo extends BasePo {
    protected Long publishId;
    protected String publishName;
    protected String address;
    protected String phonenumber;
    protected String addressCode;
}
