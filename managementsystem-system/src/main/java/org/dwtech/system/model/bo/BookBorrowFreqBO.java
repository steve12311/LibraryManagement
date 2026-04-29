package org.dwtech.system.model.bo;

import lombok.Data;

/**
 * 图书去重借阅人数统计
 *
 * @author steve12311
 * @since 2026-04-29
 */
@Data
public class BookBorrowFreqBO {
    private String isbn;
    private Integer borrowerCount;
}
