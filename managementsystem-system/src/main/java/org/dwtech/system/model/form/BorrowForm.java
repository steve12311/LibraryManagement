package org.dwtech.system.model.form;

import lombok.Data;

import java.util.Date;

@Data
public class BorrowForm {
    private String isbn;
    private Long userId;
    private Date returnTime;
    private Date realityReturnTime;
}
