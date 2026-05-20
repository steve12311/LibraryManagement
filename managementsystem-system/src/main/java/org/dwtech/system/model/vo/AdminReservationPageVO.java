package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminReservationPageVO extends BaseVO {
    private String id;
    private String isbn;
    private String bookName;
    private Long userId;
    private String nickname;
    private String username;
    private String avatar;
    private Integer status;
    private Date pickupDeadline;
    private Date createTime;
}
