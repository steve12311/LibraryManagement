package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
/**
 * DeptVO
 *
 * @author steve12311
 * @since 2025-11-18
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class DeptVO extends BaseVO {

    private Long id;

    private Long parentId;

    private String name;

    private String code;

    private Integer sort;

    private Integer status;

    private List<DeptVO> children;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
