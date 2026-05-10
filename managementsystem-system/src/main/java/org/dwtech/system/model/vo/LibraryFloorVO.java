package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;

import java.time.LocalDateTime;

/**
 * 图书馆楼层视图对象
 *
 * @author steve12311
 * @since 2026-05-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LibraryFloorVO extends BaseVO {

    private Long id;

    private Integer floorNo;

    private String name;

    private String outlineJson;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
