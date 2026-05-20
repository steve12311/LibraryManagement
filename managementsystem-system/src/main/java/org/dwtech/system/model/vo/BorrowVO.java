package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 管理端借阅视图对象
 *
 * <p>status 字段由 SQL CASE 表达式在查询时计算，取值参见 {@link org.dwtech.system.model.enums.BorrowStatus}：
 * <ul>
 *   <li>0 — 已归还（reality_return_time IS NOT NULL）</li>
 *   <li>1 — 借阅中（未归还且未超期）</li>
 *   <li>2 — 已逾期（未归还且已超期）</li>
 * </ul>
 *
 * @author steve12311
 * @since 2025-11-18
 */
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
    /** 应还日期 */
    private Date returnTime;
    /**
     * 借阅状态：0=已归还, 1=借阅中, 2=已逾期
     * 由 SQL CASE 表达式根据 reality_return_time 和 return_time 动态计算
     */
    private Integer status;
}
