package org.dwtech.system.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * 管理端借阅查询结果业务对象，对应 BorrowMapper.getBorrowPage 查询
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
    /** 应还日期 */
    private Date returnTime;
    /** 借阅状态：0=已归还, 1=借阅中, 2=已逾期，由 SQL CASE 计算 */
    private Integer status;
}
