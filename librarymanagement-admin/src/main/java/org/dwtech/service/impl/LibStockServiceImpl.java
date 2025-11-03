package org.dwtech.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.ArrayUtils;
import org.dwtech.common.core.entity.dao.LibBookStockDao;
import org.dwtech.common.core.entity.dto.LibBookStockDto;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.mapper.LibStockMapper;
import org.dwtech.service.LibStockService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LibStockServiceImpl implements LibStockService {
    private final LibStockMapper libStockMapper;

    public LibStockServiceImpl(LibStockMapper libStockMapper) {
        this.libStockMapper = libStockMapper;
    }

    @Override
    public IPage<LibBookStockDto> selectStockList(LibBookStockDto libBookStockDto) {
        Page<LibBookStockDto> page = new Page<>();
        IPage<LibBookStockDao> libBookStockDaoIPage = libStockMapper.selectStockList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), BeanUtil.copyProperties(libBookStockDto, LibBookStockDao.class));
        BeanUtil.copyProperties(libBookStockDaoIPage, page);

        List<LibBookStockDto> libBookStockDtoList = new ArrayList<>();
        libBookStockDaoIPage.getRecords().forEach(item -> {
            libBookStockDtoList.add(BeanUtil.copyProperties(item, LibBookStockDto.class));
        });
        page.setRecords(libBookStockDtoList);
        return page;
    }
}
