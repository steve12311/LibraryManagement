package org.dwtech.common.core.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class BasePojo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 创建者
     */
    protected String createBy;

    /**
     * 创建时间
     */
    protected Date createTime;

    /**
     * 更新者
     */
    protected String updateBy;

    /**
     * 更新时间
     */
    protected Date updateTime;

    /**
     * 备注
     */
    protected String remark;
}
