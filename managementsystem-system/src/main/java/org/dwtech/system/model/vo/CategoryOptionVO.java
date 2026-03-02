package org.dwtech.system.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * CategoryOptionVO
 *
 * @author steve12311
 * @since 2026-03-02
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryOptionVO {
    private Long value;

    private String label;

    /**
     * 标记当前节点是否为叶子节点。
     * true：无子节点；false：存在子节点。
     */
    private Boolean leaf;
}
