package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

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
