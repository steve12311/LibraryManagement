package org.dwtech.service.impl;

import cn.hutool.core.bean.BeanUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dto.LibPublishDto;
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
}
