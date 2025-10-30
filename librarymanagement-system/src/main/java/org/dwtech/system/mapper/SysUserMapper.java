package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.Condition;
import org.dwtech.common.core.entity.SysUser;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    SysUser selectUserByUserName(String username);

    Integer updateUser(SysUser sysUser);

    Integer updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp);

    IPage<SysUser> selectUserList(IPage<SysUser> page, @Param("sysUser") SysUser sysUser, @Param("params") Condition condition);
}
