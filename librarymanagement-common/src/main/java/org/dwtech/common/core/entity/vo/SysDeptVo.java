package org.dwtech.common.core.entity.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class SysDeptVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long deptId;
    private String deptName;
    private String leader;
    private String status;
    private Date createTime;
    private List<SysDeptVo> children;
}
