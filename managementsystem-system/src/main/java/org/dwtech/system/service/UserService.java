package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.dto.UserExportDTO;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.UserImportResultVO;
import org.dwtech.system.model.vo.UserPageVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
/**
 * 用户管理服务，提供用户认证、CRUD、导入导出、密码管理、个人信息维护等功能。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface UserService extends IService<UserPO> {
    /**
     * 根据用户名查询认证凭据，含密码、角色及数据权限范围。
     *
     * @param username 用户名
     * @return 认证凭据，用户不存在时返回 null
     */
    UserAuthCredentials getAuthCredentialsByUsername(String username);

    /**
     * 分页查询用户列表。
     *
     * @param queryParams 分页查询参数（页码、每页条数、筛选条件）
     * @return 用户分页结果
     */
    IPage<UserPageVO> getUserPage(UserPageQuery queryParams);

    /**
     * 从 Excel 流批量导入用户。逐行校验用户名、昵称、手机号、性别、角色，通过后批量写入。
     *
     * @param inputStream Excel 输入流
     * @return 导入结果（总条数、成功数、失败数及错误消息）
     */
    UserImportResultVO importUsers(InputStream inputStream);

    /**
     * 按查询条件导出用户列表（用于 Excel 导出）。
     *
     * @param queryParams 查询参数
     * @return 用户导出数据传输对象列表
     */
    List<UserExportDTO> listExportUsers(UserPageQuery queryParams);

    /**
     * 新增用户。流程：校验用户名唯一性 → 设置默认密码 → 保存用户 → 保存角色关联。
     *
     * @param formData 用户表单（用户名、昵称、角色等）
     * @return true 表示新增成功，false 表示失败
     */
    boolean saveUser(UserForm formData);

    /**
     * 更新用户信息。流程：校验用户名唯一性 → 更新用户 → 保存角色关联 → 状态变更时清除登录态。
     *
     * @param userId 用户 ID
     * @param userForm 用户表单
     * @return true 表示更新成功，false 表示失败
     */
    @Transactional
    boolean updateUser(Long userId, UserForm userForm);

    /**
     * 根据 ID 查询用户表单数据（用于编辑回显）。
     *
     * @param userId 用户 ID
     * @return 用户表单
     */
    UserForm getUserFormData(Long userId);

    /**
     * 批量删除用户。删除用户记录及对应的用户-角色关联。
     *
     * @param userIds 待删除的用户 ID 列表
     * @return true 表示全部删除成功，false 表示失败
     */
    boolean deleteUsers(List<Long> userIds);

    /**
     * 获取当前登录用户的信息，包括基础信息、角色集合和权限集合。
     *
     * @return 当前用户信息
     */
    CurrentUserVO getCurrentUserInfo();

    /**
     * 重置指定用户的密码（支持自定义密码，空值则使用系统默认密码）。
     *
     * @param userId 用户 ID
     * @param password 新密码，为空时使用系统默认密码
     * @return true 表示重置成功，false 表示失败
     */
    boolean resetPassword(Long userId, String password);

    /**
     * 更新用户状态（启用/禁用）。
     *
     * @param userId 用户 ID
     * @param status 目标状态值
     * @return true 表示更新成功，false 表示失败
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 修改密码。流程：校验原密码 → 校验新旧密码不同 → 校验确认密码 → 更新密码 → 清除登录态。
     *
     * @param userId 用户 ID
     * @param formData 密码修改表单（原密码、新密码、确认密码）
     * @return true 表示修改成功，false 表示失败
     */
    boolean changePassword(Long userId, PasswordUpdateForm formData);

    /**
     * 查询启用用户的下拉选项列表，供前端用户选择器使用。
     *
     * @return 用户选项列表（value 为用户名）
     */
    List<Option<String>> listUserOptions();

    /**
     * 获取用户个人信息。
     *
     * @param userId 用户 ID
     * @return 用户个人信息
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 更新当前登录用户的个人信息（昵称、手机号、邮箱、性别等）。
     *
     * @param formData 个人信息表单
     * @return true 表示更新成功，false 表示失败
     */
    boolean updateUserProfile(UserProfileForm formData);
}
