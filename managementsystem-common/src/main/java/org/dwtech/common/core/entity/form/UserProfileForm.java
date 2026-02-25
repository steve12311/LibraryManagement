package org.dwtech.common.core.entity.form;

import lombok.Data;

/**
 * 个人中心用户信息
 *
 * @author Ray.Hao
 * @since 2024/8/13
 */
@Data
public class UserProfileForm {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String mobile;

    private String email;

}
