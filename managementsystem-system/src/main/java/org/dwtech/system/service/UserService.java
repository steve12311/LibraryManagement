package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.PasswordUpdateForm;
import org.dwtech.common.core.entity.form.UserProfileForm;
import org.dwtech.common.core.entity.vo.CurrentUserVO;
import org.dwtech.common.core.entity.form.UserForm;
import org.dwtech.common.core.entity.po.UserPO;
import org.dwtech.common.core.entity.query.UserPageQuery;
import org.dwtech.common.core.entity.vo.UserPageVO;
import org.dwtech.common.core.entity.vo.UserProfileVO;
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
