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
 * UserService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface UserService extends IService<UserPO> {
    /**
     * 用途：获取 auth credentials by username 信息。
     * 
     * @param username username
     * @return 返回结果
     */
    UserAuthCredentials getAuthCredentialsByUsername(String username);

    /**
     * 用途：获取 user page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    IPage<UserPageVO> getUserPage(UserPageQuery queryParams);

    /**
     * 用途：导入 users。
     *
     * @param inputStream Excel 输入流
     * @return 导入结果
     */
    UserImportResultVO importUsers(InputStream inputStream);

    /**
     * 用途：导出 users 列表。
     *
     * @param queryParams query params
     * @return 导出结果列表
     */
    List<UserExportDTO> listExportUsers(UserPageQuery queryParams);

    /**
     * 用途：保存 user。
     * 
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean saveUser(UserForm formData);

    /**
     * 用途：更新 user。
     * 
     * @param userId user ID
     * @param userForm user form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Transactional
    boolean updateUser(Long userId, UserForm userForm);

    /**
     * 用途：获取 user form data 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    UserForm getUserFormData(Long userId);

    /**
     * 用途：删除 users。
     * 
     * @param userIds user ID 列表
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean deleteUsers(List<Long> userIds);

    /**
     * 用途：获取 current user info 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    CurrentUserVO getCurrentUserInfo();

    /**
     * 用途：重置 password。
     * 
     * @param userId user ID
     * @param password password
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean resetPassword(Long userId, String password);

    /**
     * 用途：更新 user status。
     * 
     * @param userId user ID
     * @param status status
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 用途：修改 password。
     * 
     * @param userId user ID
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean changePassword(Long userId, PasswordUpdateForm formData);

    /**
     * 用途：查询 user options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<Option<String>> listUserOptions();

    /**
     * 用途：获取 user profile 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 用途：更新 user profile。
     * 
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean updateUserProfile(UserProfileForm formData);
}
