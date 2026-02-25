package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * UserRolePO
 *
 * @author steve12311
 * @since 2025-11-18
 */

@TableName("sys_user_role")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRolePO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;
}
