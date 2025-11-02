package org.dwtech.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.SysUserDto;
import org.dwtech.common.core.entity.po.SysUserPo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.SysUserMapper;
import org.dwtech.system.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SysUserServiceImpl implements SysUserService {
    private final SysUserMapper sysUserMapper;

    public SysUserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public SysUserDto selectUserByUserName(String username) {
        SysUserDto sysUserDto = new SysUserDto();
        BeanUtil.copyProperties(sysUserMapper.selectUserByUserName(username), sysUserDto);
        return sysUserDto;
    }

    @Override
    public Integer updateUser(SysUserDto sysUserDto) {
        SysUserPo sysUserPo = BeanUtil.copyProperties(sysUserDto, SysUserPo.class);
        sysUserPo.setUpdateBy(SecurityUtils.getUsername());
        return sysUserMapper.updateUser(sysUserPo);
    }

    @Override
    public Integer updateLoginInfo(Long userId, String loginIp) {
        return sysUserMapper.updateLoginInfo(userId, loginIp);
    }

    @Override
    public IPage<SysUserDto> selectUserList(SysUserDto sysUserDto) {
        Page<SysUserDto> page = new Page<>();
        IPage<SysUserPo> sysUserPoIPage = sysUserMapper.selectUserList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), BeanUtil.copyProperties(sysUserDto, SysUserPo.class), PageUtils.getCondition());
        BeanUtil.copyProperties(sysUserPoIPage, page);

        List<SysUserDto> sysUserDtoList = new ArrayList<>();
        sysUserPoIPage.getRecords().forEach(record -> {
            sysUserDtoList.add(BeanUtil.copyProperties(record, SysUserDto.class));
        });
        page.setRecords(sysUserDtoList);
        return page;
    }

    @Override
    public Integer insertUser(SysUserDto sysUserDto) {
        SysUserPo sysUserPo = BeanUtil.copyProperties(sysUserDto, SysUserPo.class);
        sysUserPo.setCreateBy(SecurityUtils.getLoginUser().getUsername());
        sysUserPo.setPassword(SecurityUtils.encryptPassword(sysUserDto.getPassword()));
        return sysUserMapper.insertUser(sysUserPo);
    }

    @Override
    public Integer deleteUser(Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return 0;
        } else if (ids.length == 1) {
            return sysUserMapper.deleteUserById(ids[0]);
        }
        return 0;
    }

    @Override
    public boolean checkUserNameUnique(SysUserDto sysUserDto) {
        SysUserPo sysUserPo = new SysUserPo();
        sysUserPo.setUserName(sysUserDto.getUserName());
        return sysUserMapper.hasUser(sysUserPo) >= 1;
    }

    @Override
    public boolean checkUserPhonenumberUnique(SysUserDto sysUserDto) {
        SysUserPo sysUserPo = new SysUserPo();
        sysUserPo.setPhonenumber(sysUserDto.getPhonenumber());
        return sysUserMapper.hasUser(sysUserPo) >= 1;
    }

    @Override
    public boolean checkUserEmailUnique(SysUserDto sysUserDto) {
        SysUserPo sysUserPo = new SysUserPo();
        sysUserPo.setEmail(sysUserDto.getEmail());
        return sysUserMapper.hasUser(sysUserPo) >= 1;
    }

    @Override
    public boolean hasUserById(SysUserDto sysUserDto) {
        SysUserPo sysUserPo = new SysUserPo();
        sysUserPo.setUserId(sysUserDto.getUserId());
        return sysUserMapper.hasUser(sysUserPo) >= 1;
    }

    @Override
    public boolean checkIdsExist(Long[] ids) {
        return sysUserMapper.hasUserByIds(ids) == ids.length;
    }
}
