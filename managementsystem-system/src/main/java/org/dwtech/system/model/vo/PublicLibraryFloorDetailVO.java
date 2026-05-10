package org.dwtech.system.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 公开楼层地图详情
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class PublicLibraryFloorDetailVO {

    private Long id;

    private Integer floorNo;

    private String name;

    private String outlineJson;

    private List<PublicBookshelfVO> shelves;
}
