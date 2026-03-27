package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 热门图书排行项
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardHotBookVO {
    private String isbn;
    private String bookName;
    private String cover;
    private Long count;
}
