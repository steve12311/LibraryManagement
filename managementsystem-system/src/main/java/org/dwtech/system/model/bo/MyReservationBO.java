package org.dwtech.system.model.bo;

import lombok.Data;
import java.util.Date;

@Data
public class MyReservationBO {
    private String id;
    private String isbn;
    private String cover;
    private String bookName;
    private Integer status;
    private Date pickupDeadline;
    private Date createTime;
}
