package org.dwtech.system.model.bo;

import lombok.Data;

import java.util.Date;
/**
 * BorrowBO
 *
 * @author steve12311
 * @since 2026-02-24
 */

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
