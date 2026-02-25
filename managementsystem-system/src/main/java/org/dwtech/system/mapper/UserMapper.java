package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.annontation.DataPermission;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.system.model.bo.UserBO;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.query.UserPageQuery;
/**
 * UserMapper
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
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
     * @param page page
     * @param queryParams query params
     * @return 分页结果
     */
    @DataPermission(deptAlias = "u", userAlias = "u")
    Page<UserBO> getUserPage(Page<UserBO> page, @Param("queryParams") UserPageQuery queryParams);

    /**
     * 用途：获取 user form data 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    UserForm getUserFormData(Long userId);

    /**
     * 用途：获取 user profile 信息。
     * 
     * @param userId user ID
     * @return 返回结果
     */
    UserBO getUserProfile(Long userId);
}
