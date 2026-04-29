package org.dwtech.system.model.bo;

import lombok.Data;

/**
 * 两本图书被同一批用户共同借阅的次数统计
 *
 * @author steve12311
 * @since 2026-04-29
 */
@Data
public class CoBorrowCountBO {
    private String isbnA;
    private String isbnB;
    private Integer coBorrowCount;
}
