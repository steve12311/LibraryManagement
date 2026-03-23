package org.dwtech.system.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * 当前登录用户借阅订单业务对象
 *
 * @author steve12311
 * @since 2026-03-23
 */
@Data
public class MyBorrowBO {
    private String id;
    private String isbn;
    private String cover;
    private String bookName;
    private Date returnTime;
    private Integer status;
}
