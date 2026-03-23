package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.SystemConstants;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.system.model.bo.UserBO;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.UserPageVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.service.PermissionService;
import org.dwtech.common.token.TokenManager;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.converter.UserConverter;
import org.dwtech.system.mapper.UserMapper;
import org.dwtech.system.service.RoleService;
import org.dwtech.system.service.UserRoleService;
import org.dwtech.system.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
/**
 * UserServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserPO> implements UserService {
    private final RoleService roleService;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final TokenManager tokenManager;

    /**
     * 用途：获取 auth credentials by username 信息。
     * 
     * @param username username
     * @return 返回结果
     */
    @Override
    public UserAuthCredentials getAuthCredentialsByUsername(String username) {
        UserAuthCredentials userAuthCredentials = this.baseMapper.getAuthCredentialsByUsername(username);
        if (userAuthCredentials != null) {
            Set<String> roles = userAuthCredentials.getRoles();
            // 获取最大范围的数据权限
            Integer dataScope = roleService.getMaximumDataScope(roles);
            userAuthCredentials.setDataScope(dataScope);
        }
        return userAuthCredentials;
    }

    /**
     * 用途：获取 user page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    @Override
    public IPage<UserPageVO> getUserPage(UserPageQuery queryParams) {
        // 参数构建
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<UserBO> page = new Page<>(pageNum, pageSize);

        boolean isRoot = SecurityUtils.isRoot();
        queryParams.setIsRoot(isRoot);
        Page<UserBO> userPage = this.baseMapper.getUserPage(page, queryParams);
        return userConverter.toPageVo(userPage);
    }

    /**
     * 用途：保存 user。
     * 
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @Transactional
    public boolean saveUser(UserForm formData) {
        String username = formData.getUsername();

        long count = this.count(this.lambdaQuery().eq(UserPO::getUsername, username));
        Assert.isTrue(count == 0, "用户名已存在");

        // 实体转换 form->po
        UserPO po = userConverter.toPo(formData);

        // 设置默认加密密码
        String defaultEncryptPwd = passwordEncoder.encode(SystemConstants.DEFAULT_PASSWORD);
        po.setPassword(defaultEncryptPwd);
        po.setCreateBy(SecurityUtils.getUserId());

        boolean result = this.save(po);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(po.getId(), formData.getRoleIds());
        }

        return result;
    }

    /**
     * 用途：更新 user。
     * 
     * @param userId user ID
     * @param userForm user form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @Transactional
    public boolean updateUser(Long userId, UserForm userForm) {

        String username = userForm.getUsername();

        long count = this.count(
                new LambdaQueryWrapper<UserPO>()
                        .eq(UserPO::getUsername, username)
                        .ne(UserPO::getId, userId)
        );
        Assert.isTrue(count == 0, "用户名已存在");

        // form -> entity
        UserPO entity = userConverter.toPo(userForm);
        entity.setUpdateBy(SecurityUtils.getUserId());

        // 修改用户
        boolean result = this.updateById(entity);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 用途：获取 user form data 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    @Override
    public UserForm getUserFormData(Long userId) {
        return this.baseMapper.getUserFormData(userId);
    }

    /**
     * 用途：删除 users。
     * 
     * @param userIds user ID 列表
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @Transactional
    public boolean deleteUsers(List<Long> userIds) {
        Assert.isTrue(ArrayUtil.isNotEmpty(userIds), "删除的用户数据为空");
        boolean result = this.removeByIds(userIds);
        Assert.isTrue(result, "删除用户失败");
        userRoleService.removeUserRolesByUserIds(userIds);
        return true;
    }

    /**
     * 用途：获取 current user info 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @Override
    public CurrentUserVO getCurrentUserInfo() {
        String username = SecurityUtils.getUsername();

        // 获取登录用户基础信息
        UserPO user = this.getOne(new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getUsername, username)
                .select(
                        UserPO::getId,
                        UserPO::getUsername,
                        UserPO::getNickname,
                        UserPO::getAvatar
                )
        );
        // entity->VO
        CurrentUserVO userInfoVO = userConverter.toCurrentUser(user);

        // 用户角色集合
        Set<String> roles = SecurityUtils.getRoles();
        userInfoVO.setRoles(roles);

        // 用户权限集合
        if (CollectionUtil.isNotEmpty(roles)) {
            Set<String> perms = permissionService.getRolePermsFromCache(roles);
            userInfoVO.setPerms(perms);
        }
        return userInfoVO;
    }

    /**
     * 用途：重置 password。
     * 
     * @param userId user ID
     * @param password password
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    public boolean resetPassword(Long userId, String password) {
        if (StrUtil.isBlank(password)) {
            password = SystemConstants.DEFAULT_PASSWORD;
        }
        boolean result = this.update(
                new LambdaUpdateWrapper<UserPO>()
                        .eq(UserPO::getId, userId)
                        .set(UserPO::getPassword, passwordEncoder.encode(password))
        );
        if (result) {
            tokenManager.invalidateUserSessions(userId);
        }
        return result;
    }

    /**
     * 用途：更新 user status。
     * 
     * @param userId user ID
     * @param status status
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        boolean result = this.update(
                new LambdaUpdateWrapper<UserPO>()
                        .eq(UserPO::getId, userId)
                        .set(UserPO::getStatus, status));
        if (result) {
            tokenManager.invalidateUserSessions(userId);
        }
        return result;
    }

    /**
     * 用途：修改 password。
     * 
     * @param userId user ID
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    public boolean changePassword(Long userId, PasswordUpdateForm formData) {
        UserPO user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在!");
        }

        // 校验原密码
        if (!passwordEncoder.matches(formData.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // 新旧密码不能相同
        if (passwordEncoder.matches(formData.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 判断新密码和确认密码是否一致
        if (!StrUtil.equals(formData.getNewPassword(), formData.getConfirmPassword())) {
            throw new BusinessException("新密码和确认密码不一致");
        }

        boolean result = this.update(
                new LambdaUpdateWrapper<UserPO>()
                        .eq(UserPO::getId, userId)
                        .set(UserPO::getPassword, passwordEncoder.encode(formData.getNewPassword()))
        );

        if (result) {
            tokenManager.invalidateUserSessions(userId);
        }
        return result;
    }

    /**
     * 用途：查询 user options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    @Override
    public List<Option<String>> listUserOptions() {
        List<UserPO> list = this.list(
                new LambdaQueryWrapper<UserPO>()
                        .eq(UserPO::getStatus, 1)
        );
        return userConverter.toOptions(list);
    }

    /**
     * 用途：获取 user profile 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    @Override
    public UserProfileVO getUserProfile(Long userId) {
        UserBO entity = this.baseMapper.getUserProfile(userId);
        return userConverter.toProfileVo(entity);
    }

    /**
     * 用途：更新 user profile。
     * 
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    public boolean updateUserProfile(UserProfileForm formData) {
        Long userId = SecurityUtils.getUserId();
        UserPO entity = userConverter.toPo(formData);
        entity.setId(userId);
        return this.updateById(entity);
    }
}
