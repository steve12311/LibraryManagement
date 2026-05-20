package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReservationPageVO extends BaseVO {
    private String id;
    private String isbn;
    private String cover;
    private String bookName;
    private Integer status;
    private Date pickupDeadline;
    private Date createTime;
}
