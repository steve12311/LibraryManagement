package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDeptPo extends BasePo {
    protected Long deptId;
    protected Long parentId;
    protected String ancestors;
    protected String deptName;
    protected Integer orderNum;
    protected String leader;
    protected String phone;
    protected String email;
    protected String status;
    protected String delFlag;
}
