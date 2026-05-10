package org.dwtech.system.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 公开书架地图对象
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class PublicBookshelfVO {

    private Long shelfId;

    private String shelfNo;

    private String name;

    private BigDecimal x;

    private BigDecimal y;

    private BigDecimal width;

    private BigDecimal height;

    private BigDecimal angle;

    private Integer capacity;

    private Integer usedStock;

    private List<PublicShelfBookVO> books;
}
