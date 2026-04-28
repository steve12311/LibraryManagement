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

import java.util.List;
/**
 * 用户数据访问层，提供用户信息的查询和数据权限接口
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
    /**
     * 根据用户名查询用户认证凭据
     *
     * @return 用户认证凭据
     */
    UserAuthCredentials getAuthCredentialsByUsername(String username);

    /**
     * 分页查询用户列表，支持数据权限过滤
     *
     * @return 分页结果
     */
    @DataPermission(deptAlias = "u", userAlias = "u")
    Page<UserBO> getUserPage(Page<UserBO> page, @Param("queryParams") UserPageQuery queryParams);

    /**
     * 导出用户列表，支持数据权限过滤
     *
     * @return 导出列表
     */
    @DataPermission(deptAlias = "u", userAlias = "u")
    List<UserBO> listExportUsers(@Param("queryParams") UserPageQuery queryParams);

    /**
     * 根据用户 ID 查询用户表单数据
     *
     * @return 用户表单数据
     */
    UserForm getUserFormData(Long userId);

    /**
     * 根据用户 ID 查询用户详细信息
     *
     * @return 用户详细信息
     */
    UserBO getUserProfile(Long userId);
}
