package org.dwtech.system.model.form;

import lombok.Data;

/**
 * 个人中心用户信息
 *
 * @author steve12311
* @since 2026-02-25
 */
/**
 * UserProfileForm
 *
 * @author steve12311
 * @since 2026-02-25
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
