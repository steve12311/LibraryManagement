package org.dwtech.common.core.entity.form;

import lombok.Data;

import java.util.Date;

@Data
public class BorrowForm {
    private String isbn;
    private Long userId;
    private Date returnTime;
    private Date realityReturnTime;
}
