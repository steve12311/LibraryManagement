package org.dwtech.system.model.form;

import lombok.Data;

import java.util.Date;
/**
 * BorrowForm
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Data
public class BorrowForm {
    private String isbn;
    private Long userId;
    private Date returnTime;
    private Date realityReturnTime;
}
