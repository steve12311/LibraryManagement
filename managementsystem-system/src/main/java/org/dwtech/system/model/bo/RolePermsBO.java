package org.dwtech.system.model.bo;

import lombok.Data;

import java.util.Set;

/**
 * 角色权限业务对象
 *
 * @author steve12311
* @since 2025-11-18
 */
/**
 * RolePermsBO
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class RolePermsBO {

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 权限标识集合
     */
    private Set<String> perms;

}
