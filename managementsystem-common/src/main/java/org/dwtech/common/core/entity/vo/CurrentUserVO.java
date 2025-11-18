package org.dwtech.common.core.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * 当前登录用户对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CurrentUserVO extends BaseVO{

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private Set<String> roles;

    private Set<String> perms;

}
