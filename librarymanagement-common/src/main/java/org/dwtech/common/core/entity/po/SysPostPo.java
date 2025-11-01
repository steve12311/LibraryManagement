package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysPostPo extends BasePo {
    private Long postId;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String status;
}
