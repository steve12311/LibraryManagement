package org.dwtech.common.core.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

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
