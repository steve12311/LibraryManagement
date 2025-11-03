package org.dwtech.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.LibPublishDto;
import org.dwtech.common.core.entity.po.LibPublishPo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.mapper.LibPublishMapper;
import org.dwtech.service.LibPublishService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LibPublishServiceImpl implements LibPublishService {
    private final LibPublishMapper libPublishMapper;

    public LibPublishServiceImpl(LibPublishMapper libPublishMapper) {
        this.libPublishMapper = libPublishMapper;
    }

    @Override
    public List<LibPublishDto> selectPublishByIds(Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return List.of();
        }
        List<LibPublishDto> publishDtoList = new ArrayList<>();
        libPublishMapper.selectPublishByIds(ids).forEach(publish -> {
            publishDtoList.add(BeanUtil.copyProperties(publish, LibPublishDto.class));
        });
        return publishDtoList;
    }

    @Override
    public IPage<LibPublishDto> selectPublishList(LibPublishDto libPublishDto) {
        Page<LibPublishDto> page = new Page<>();
        IPage<LibPublishPo> publishPoList = libPublishMapper.selectPublishList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), BeanUtil.copyProperties(libPublishDto, LibPublishPo.class));
        BeanUtil.copyProperties(publishPoList, page);

        List<LibPublishDto> publishDtoList = new ArrayList<>();
        publishPoList.getRecords().forEach(publish -> {
            publishDtoList.add(BeanUtil.copyProperties(publish, LibPublishDto.class));
        });
        page.setRecords(publishDtoList);
        return page;
    }

    @Override
    public Integer insertPublish(LibPublishDto libPublishDto) {
        LibPublishPo publishPo = BeanUtil.copyProperties(libPublishDto, LibPublishPo.class);
        publishPo.setCreateBy(SecurityUtils.getUsername());
        return libPublishMapper.insertPublish(publishPo);
    }

    @Override
    public Integer updatePublish(LibPublishDto libPublishDto) {
        LibPublishPo publishPo = BeanUtil.copyProperties(libPublishDto, LibPublishPo.class);
        publishPo.setUpdateBy(SecurityUtils.getUsername());
        return libPublishMapper.updatePublish(publishPo);
    }

    @Override
    public Integer deletePublish(Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return 0;
        } else if (ids.length == 1) {
            return libPublishMapper.deletePublish(ids[0]);
        }
        return 0;
    }
}
