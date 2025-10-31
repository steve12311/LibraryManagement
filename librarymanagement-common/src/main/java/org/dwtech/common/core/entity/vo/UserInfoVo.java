package org.dwtech.common.core.entity.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
public class UserInfoVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private String deptName;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String sex;
    private String avatar;
    private String status;
    private Set<String> roles;
    private Set<String> posts;
}
