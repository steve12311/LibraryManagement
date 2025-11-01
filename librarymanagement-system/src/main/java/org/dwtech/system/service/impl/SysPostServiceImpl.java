package org.dwtech.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.SysPostDto;
import org.dwtech.common.core.entity.po.SysPostPo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.SysPostMapper;
import org.dwtech.system.service.SysPostService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysPostServiceImpl implements SysPostService {
    private final SysPostMapper sysPostMapper;

    public SysPostServiceImpl(SysPostMapper sysPostMapper) {
        this.sysPostMapper = sysPostMapper;
    }

    @Override
    public IPage<SysPostDto> selectPostList(SysPostDto sysPostDto) {
        SysPostPo sysPostPo = new SysPostPo();
        BeanUtil.copyProperties(sysPostDto, sysPostPo);

        Page<SysPostDto> page = new Page<>();
        IPage<SysPostPo> sysPostPoIPage = sysPostMapper.selectPostList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), sysPostPo);
        BeanUtil.copyProperties(sysPostPoIPage, page);

        List<SysPostDto> sysPostDtoList = new ArrayList<>();
        sysPostPoIPage.getRecords().forEach(sysPostPo1 -> {
            SysPostDto sysPostDto1 = new SysPostDto();
            BeanUtil.copyProperties(sysPostPo1, sysPostDto1);
            sysPostDtoList.add(sysPostDto1);
        });
        page.setRecords(sysPostDtoList);
        return page;
    }

    @Override
    public boolean checkPostNameUnique(SysPostDto sysPostDto) {
        SysPostPo sysPostPo = new SysPostPo();
        BeanUtil.copyProperties(sysPostDto, sysPostPo);
        sysPostPo.setPostName(sysPostDto.getPostName());
        return sysPostMapper.hasPost(sysPostPo) >= 1;
    }

    @Override
    public Integer insertPost(SysPostDto postDto) {
        SysPostPo sysPostPo = new SysPostPo();
        BeanUtil.copyProperties(postDto, sysPostPo);
        sysPostPo.setCreateBy(SecurityUtils.getUsername());
        return sysPostMapper.insertPost(sysPostPo);
    }

    @Override
    public Integer updatePost(SysPostDto sysPostDto) {
        SysPostPo sysPostPo = new SysPostPo();
        BeanUtil.copyProperties(sysPostDto, sysPostPo);
        sysPostPo.setUpdateBy(SecurityUtils.getUsername());
        return sysPostMapper.updatePost(sysPostPo);
    }

    @Override
    public Integer deletePost(Long[] postIds) {
        if (ArrayUtils.isEmpty(postIds)) {
            return 0;
        } else if (postIds.length == 1) {
            return sysPostMapper.deletePostById(postIds[0]);
        }
        return 0;
    }
}
