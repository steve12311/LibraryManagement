package org.dwtech.system.model.query;

import lombok.Data;
/**
 * CategoryQuery
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Data
public class CategoryQuery {
    private Long parentId;

    private String categoryName;

    private Integer status;
}
