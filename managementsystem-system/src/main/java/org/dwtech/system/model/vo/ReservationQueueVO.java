package org.dwtech.system.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseVO;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReservationQueueVO extends BaseVO {
    private String id;
    private Long userId;
    private String nickname;
    private String username;
    private Integer status;
    private Date createTime;
}
