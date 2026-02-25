package org.dwtech.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 个人中心用户信息
 *
 * @author Ray
 * @since 2024/8/13
 */
@Data
public class UserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String mobile;

    private String email;

    // private String deptName;

    private String roleNames;

    private Date createTime;

}
