package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;

import java.util.Date;

/**
 * 当前登录用户借阅订单分页视图对象
 *
 * @author steve12311
 * @since 2026-03-23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyBorrowPageVO extends BaseVO {
    private String borrowId;
    private String isbn;
    private String cover;
    private String bookName;
    private Date returnTime;
    private Integer status;
}
