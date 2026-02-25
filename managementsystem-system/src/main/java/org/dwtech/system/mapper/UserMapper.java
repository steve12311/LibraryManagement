package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.annontation.DataPermission;
import org.dwtech.common.core.entity.UserAuthCredentials;
import org.dwtech.common.core.entity.bo.UserBO;
import org.dwtech.common.core.entity.form.UserForm;
import org.dwtech.common.core.entity.po.UserPO;
import org.dwtech.common.core.entity.query.UserPageQuery;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
    UserAuthCredentials getAuthCredentialsByUsername(String username);

    @DataPermission(deptAlias = "u", userAlias = "u")
    Page<UserBO> getUserPage(Page<UserBO> page, @Param("queryParams") UserPageQuery queryParams);

    UserForm getUserFormData(Long userId);

    UserBO getUserProfile(Long userId);
}
