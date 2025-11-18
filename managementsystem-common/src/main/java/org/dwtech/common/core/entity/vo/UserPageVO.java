package org.dwtech.common.core.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户分页视图对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageVO extends BaseVO {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Integer gender;

    private String avatar;

    private String email;

    private Integer status;

    private String deptName;

    private String roleNames;

    private LocalDateTime createTime;

}
