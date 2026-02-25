package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.UserPageVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService extends IService<UserPO> {
    UserAuthCredentials getAuthCredentialsByUsername(String username);

    IPage<UserPageVO> getUserPage(UserPageQuery queryParams);

    boolean saveUser(UserForm formData);

    @Transactional
    boolean updateUser(Long userId, UserForm userForm);

    UserForm getUserFormData(Long userId);

    boolean deleteUsers(List<Long> userIds);

    CurrentUserVO getCurrentUserInfo();

    boolean resetPassword(Long userId, String password);

    boolean updateUserStatus(Long userId, Integer status);

    boolean changePassword(Long userId, PasswordUpdateForm formData);

    List<Option<String>> listUserOptions();

    UserProfileVO getUserProfile(Long userId);

    boolean updateUserProfile(UserProfileForm formData);
}
