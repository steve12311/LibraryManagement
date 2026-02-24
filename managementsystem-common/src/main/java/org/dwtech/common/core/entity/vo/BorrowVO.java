package org.dwtech.common.core.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class BorrowVO extends BaseVO {
    private String borrowId;
    private String isbn;
    private String bookName;
    private Long userId;
    private String nickname;
    private String username;
    private String avatar;
    private Date returnTime;
    private Date realityReturnTime;
}
