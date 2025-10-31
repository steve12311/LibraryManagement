package org.dwtech.common.core.entity.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysUserVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private String userName;
    private String nickName;
    private String deptName;
    private String phonenumber;
    private String status;
    private Date createTime;
}
