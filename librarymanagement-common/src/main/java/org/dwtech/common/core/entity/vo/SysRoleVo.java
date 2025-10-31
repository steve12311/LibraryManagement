package org.dwtech.common.core.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SysRoleVo implements Serializable {
    private Long roleId;
    private String roleName;
    private String roleKey;
    private String status;
    private Date createTime;
}
