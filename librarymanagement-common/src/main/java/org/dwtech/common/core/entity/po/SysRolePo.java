package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRolePo extends BasePo {
    protected Long roleId;
    protected String roleName;
    protected String roleKey;
    protected Integer roleSort;
    protected String dataScope;
    protected String status;
    protected String delFlag;
}
