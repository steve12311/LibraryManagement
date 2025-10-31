package org.dwtech.common.core.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dwtech.common.valid.SysAddUserGroup;
import org.dwtech.common.valid.SysEditUserGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysUserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 用户ID
     */
    @NotNull(groups = SysEditUserGroup.class, message = "用户ID不能为空")
    private Long userId;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 用户账号
     */
    @NotBlank(groups = SysAddUserGroup.class, message = "用户名不能为空")
    private String userName;
    /**
     * 用户昵称
     */
    private String nickName;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 用户头像
     */
    protected String avatar;
    /**
     * 手机号码
     */
    private String phonenumber;
    /**
     * 用户性别
     */
    private String sex;
    /**
     * 密码
     */
    @NotBlank(groups = SysAddUserGroup.class, message = "密码不能为空")
    private String password;
    /**
     * 账号状态（0正常 1停用）
     */
    private String status;
    /**
     * 密码最后更新时间
     */
    private Date pwdUpdateDate;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 角色组
     */
    private Long[] roleIds;
    /**
     * 岗位组
     */
    private Long[] postIds;
    /**
     * 备注
     */
    private String remark;
}
