package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
/**
 * RolePageVO
 *
 * @author steve12311
 * @since 2025-11-18
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class RolePageVO extends BaseVO {

    private Long id;

    private String name;

    private String code;

    private Integer status;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
