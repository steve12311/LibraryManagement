package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 书架下拉选项
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class BookshelfOptionVO {

    private Long value;

    private Long id;

    private String label;

    private String shelfNo;

    private String name;

    private Long floorId;

    private String floorName;

    private Integer capacity;

    private Integer usedStock;

    private Integer remainingCapacity;

    private Integer status;
}
