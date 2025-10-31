package org.dwtech.common.core.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserPo extends BasePo {
    /**
     * 用户ID
     */
    protected Long userId;

    /**
     * 部门ID
     */
    protected Long deptId;

    /**
     * 用户账号
     */
    protected String userName;

    /**
     * 用户昵称
     */
    protected String nickName;

    /**
     * 用户邮箱
     */
    protected String email;

    /**
     * 手机号码
     */
    protected String phonenumber;

    /**
     * 用户性别
     */
    protected String sex;

    /**
     * 用户头像
     */
    protected String avatar;

    /**
     * 密码
     */
    protected String password;

    /**
     * 账号状态（0正常 1停用）
     */
    protected String status;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    protected String delFlag;

    /**
     * 最后登录IP
     */
    protected String loginIp;

    /**
     * 最后登录时间
     */
    protected Date loginDate;

    /**
     * 密码最后更新时间
     */
    protected Date pwdUpdateDate;
}
