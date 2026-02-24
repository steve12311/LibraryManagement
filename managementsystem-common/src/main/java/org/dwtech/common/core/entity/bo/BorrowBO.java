package org.dwtech.common.core.entity.bo;

import lombok.Data;

import java.util.Date;

@Data
public class BorrowBO {
    private String id;
    private String isbn;
    private String bookName;
    private Long userId;
    private String nickname;
    private String username;
    private String avatar;
    private Date returnTime;
    private Date realityReturnTime;
}
