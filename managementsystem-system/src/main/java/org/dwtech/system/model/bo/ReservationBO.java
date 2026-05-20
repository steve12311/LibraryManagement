package org.dwtech.system.model.bo;

import lombok.Data;
import java.util.Date;

@Data
public class ReservationBO {
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
