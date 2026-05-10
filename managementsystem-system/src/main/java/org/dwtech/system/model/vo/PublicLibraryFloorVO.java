package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 公开楼层选项
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class PublicLibraryFloorVO {

    private Long id;

    private Integer floorNo;

    private String name;

    private Integer sort;
}
