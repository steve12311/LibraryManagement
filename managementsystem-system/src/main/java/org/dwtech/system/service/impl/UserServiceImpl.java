package org.dwtech.system.service.impl;

import cn.idev.excel.EasyExcel;
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
import org.dwtech.common.model.Option;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.system.model.form.PasswordUpdateForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.bo.UserBO;
import org.dwtech.system.model.dto.UserExportDTO;
import org.dwtech.system.model.dto.UserImportDTO;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.entity.UserRolePO;
import org.dwtech.system.model.query.UserPageQuery;
import org.dwtech.system.model.vo.UserImportResultVO;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    private static final int IMPORT_BATCH_SIZE = 100;
    private static final Pattern MOBILE_PATTERN = Pattern.compile(
            "^1(3\\d|4[5-9]|5[0-35-9]|6[2567]|7[0-8]|8\\d|9[0-35-9])\\d{8}$"
    );

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
     * 用途：导入 users。
     *
     * @param inputStream Excel 输入流
     * @return 导入结果
     */
    @Override
    @Transactional
    public UserImportResultVO importUsers(InputStream inputStream) {
        if (inputStream == null) {
            throw new BusinessException(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY, "导入文件不能为空");
        }

        List<UserImportDTO> rows = readImportRows(inputStream);
        List<UserImportDTO> importRows = rows.stream()
                .filter(row -> !isEmptyImportRow(row))
                .toList();

        UserImportResultVO result = new UserImportResultVO();
        result.setTotalCount(importRows.size());
        if (CollectionUtil.isEmpty(importRows)) {
            return result;
        }

        Map<String, Long> roleNameMap = buildRoleNameMap();
        Set<String> existingUsernames = loadExistingUsernames(importRows);
        Set<String> seenUsernames = new HashSet<>();
        List<ImportUserRow> validRows = new ArrayList<>();

        for (int index = 0; index < importRows.size(); index++) {
            UserImportDTO row = importRows.get(index);
            int rowNumber = index + 2;
            ImportUserRow validRow = validateImportRow(
                    row,
                    rowNumber,
                    roleNameMap,
                    existingUsernames,
                    seenUsernames,
                    result.getMessages()
            );
            if (validRow != null) {
                validRows.add(validRow);
            }
        }

        result.setFailureCount(result.getMessages().size());
        if (CollectionUtil.isEmpty(validRows)) {
            return result;
        }

        List<UserPO> users = validRows.stream()
                .map(ImportUserRow::user)
                .toList();
        boolean saved = this.saveBatch(users, IMPORT_BATCH_SIZE);
        if (!saved) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "批量导入用户失败");
        }

        Map<String, Long> importedUserIdMap = this.list(new LambdaQueryWrapper<UserPO>()
                        .in(UserPO::getUsername, users.stream().map(UserPO::getUsername).toList())
                        .select(UserPO::getId, UserPO::getUsername))
                .stream()
                .collect(Collectors.toMap(UserPO::getUsername, UserPO::getId));

        List<UserRolePO> userRoles = new ArrayList<>();
        for (ImportUserRow validRow : validRows) {
            Long userId = importedUserIdMap.get(validRow.user().getUsername());
            if (userId == null) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "导入用户后无法回查用户ID");
            }
            validRow.roleIds().stream()
                    .map(roleId -> new UserRolePO(userId, roleId))
                    .forEach(userRoles::add);
        }
        userRoleService.saveBatchUserRoles(userRoles, IMPORT_BATCH_SIZE);

        result.setSuccessCount(validRows.size());
        result.setFailureCount(result.getTotalCount() - result.getSuccessCount());
        return result;
    }

    /**
     * 用途：导出 users 列表。
     *
     * @param queryParams query params
     * @return 导出结果列表
     */
    @Override
    public List<UserExportDTO> listExportUsers(UserPageQuery queryParams) {
        queryParams.setIsRoot(SecurityUtils.isRoot());
        List<UserBO> exportUsers = this.baseMapper.listExportUsers(queryParams);
        if (CollectionUtil.isEmpty(exportUsers)) {
            return Collections.emptyList();
        }

        return userConverter.toExportDtos(exportUsers);
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
        UserPO currentUser = this.getById(userId);

        long count = this.count(
                new LambdaQueryWrapper<UserPO>()
                        .eq(UserPO::getUsername, username)
                        .ne(UserPO::getId, userId)
        );
        Assert.isTrue(count == 0, "用户名已存在");

        // form -> entity
        UserPO entity = userConverter.toPo(userForm);
        // 更新目标一律以路径 userId 为准，避免请求体中的 id 干扰更新对象。
        entity.setId(userId);
        entity.setUpdateBy(SecurityUtils.getUserId());
        boolean statusChanged = currentUser != null
                && entity.getStatus() != null
                && !Objects.equals(currentUser.getStatus(), entity.getStatus());

        // 修改用户
        boolean result = this.updateById(entity);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(userId, userForm.getRoleIds());
            if (statusChanged) {
                tokenManager.invalidateUserSessions(userId);
            }
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

    private List<UserImportDTO> readImportRows(InputStream inputStream) {
        try {
            return EasyExcel.read(inputStream)
                    .head(UserImportDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("user_import_parse_failed error={}", ex.getClass().getSimpleName(), ex);
            throw new BusinessException(ResultCode.INVALID_USER_INPUT, "Excel 文件解析失败");
        }
    }

    private boolean isEmptyImportRow(UserImportDTO row) {
        return StrUtil.isAllBlank(
                row.getUsername(),
                row.getNickname(),
                row.getGenderLabel(),
                row.getMobile(),
                row.getEmail(),
                row.getRoleNames()
        );
    }

    private Map<String, Long> buildRoleNameMap() {
        Map<String, Long> roleNameMap = new HashMap<>();
        for (Option<Long> option : roleService.listRoleOptions()) {
            if (option == null || option.getValue() == null || StrUtil.isBlank(option.getLabel())) {
                continue;
            }
            roleNameMap.put(StrUtil.trim(option.getLabel()), option.getValue());
        }
        return roleNameMap;
    }

    private Set<String> loadExistingUsernames(List<UserImportDTO> importRows) {
        Set<String> usernames = importRows.stream()
                .map(UserImportDTO::getUsername)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .collect(Collectors.toSet());
        if (CollectionUtil.isEmpty(usernames)) {
            return Collections.emptySet();
        }

        return this.list(new LambdaQueryWrapper<UserPO>()
                        .in(UserPO::getUsername, usernames)
                        .select(UserPO::getUsername))
                .stream()
                .map(UserPO::getUsername)
                .collect(Collectors.toSet());
    }

    private ImportUserRow validateImportRow(
            UserImportDTO row,
            int rowNumber,
            Map<String, Long> roleNameMap,
            Set<String> existingUsernames,
            Set<String> seenUsernames,
            List<String> messages
    ) {
        List<String> errors = new ArrayList<>();

        String username = StrUtil.trim(row.getUsername());
        if (StrUtil.isBlank(username)) {
            errors.add("用户名不能为空");
        } else {
            if (!seenUsernames.add(username)) {
                errors.add("导入文件中用户名重复");
            }
            if (existingUsernames.contains(username)) {
                errors.add("用户名已存在");
            }
        }

        String nickname = StrUtil.trim(row.getNickname());
        if (StrUtil.isBlank(nickname)) {
            errors.add("昵称不能为空");
        }

        String mobile = StrUtil.trim(row.getMobile());
        if (StrUtil.isNotBlank(mobile) && !MOBILE_PATTERN.matcher(mobile).matches()) {
            errors.add("手机号码格式不正确");
        }

        Integer gender = parseGender(row.getGenderLabel(), errors);
        List<Long> roleIds = parseRoleIds(row.getRoleNames(), roleNameMap, errors);

        if (CollectionUtil.isNotEmpty(errors)) {
            messages.add("第" + rowNumber + "行：" + String.join("；", errors));
            return null;
        }

        UserPO user = new UserPO();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setMobile(mobile);
        user.setEmail(StrUtil.trim(row.getEmail()));
        user.setGender(gender);
        user.setPassword(passwordEncoder.encode(SystemConstants.DEFAULT_PASSWORD));
        user.setCreateBy(SecurityUtils.getUserId());
        return new ImportUserRow(user, roleIds);
    }

    private Integer parseGender(String genderLabel, List<String> errors) {
        String gender = StrUtil.trim(genderLabel);
        if (StrUtil.isBlank(gender)) {
            return null;
        }
        return switch (gender) {
            case "男" -> 1;
            case "女" -> 2;
            case "保密" -> 0;
            default -> {
                errors.add("性别仅支持 男、女、保密");
                yield null;
            }
        };
    }

    private List<Long> parseRoleIds(String roleNames, Map<String, Long> roleNameMap, List<String> errors) {
        if (StrUtil.isBlank(roleNames)) {
            errors.add("角色不能为空");
            return Collections.emptyList();
        }

        LinkedHashSet<Long> roleIds = new LinkedHashSet<>();
        for (String roleName : StrUtil.split(roleNames.replace('，', ','), ',')) {
            String normalizedRoleName = StrUtil.trim(roleName);
            if (StrUtil.isBlank(normalizedRoleName)) {
                continue;
            }
            Long roleId = roleNameMap.get(normalizedRoleName);
            if (roleId == null) {
                errors.add("角色【" + normalizedRoleName + "】不存在或不可分配");
                continue;
            }
            roleIds.add(roleId);
        }

        if (CollectionUtil.isEmpty(roleIds)) {
            errors.add("角色不能为空");
        }
        return new ArrayList<>(roleIds);
    }

    private record ImportUserRow(UserPO user, List<Long> roleIds) {
    }
}
