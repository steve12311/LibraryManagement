package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.common.core.entity.Condition;
import org.dwtech.common.core.entity.po.SysUserPo;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPo> {
    SysUserPo selectUserByUserName(String username);

    Integer updateUser(SysUserPo sysUserPo);

    Integer updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp);

    IPage<SysUserPo> selectUserList(IPage<SysUserPo> page, @Param("sysUser") SysUserPo sysUser, @Param("params") Condition condition);

    Integer insertUser(SysUserPo sysUserPo);

    Integer deleteUserById(Long ids);

    Integer hasUser(SysUserPo sysUserPo);

    Integer hasUserByIds(Long[] ids);
}
