package org.dwtech.system.model.bo;

import lombok.Data;

/**
 * 书架占用量统计
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class BookshelfUsageBO {

    private Long shelfId;

    private Integer usedStock;
}
