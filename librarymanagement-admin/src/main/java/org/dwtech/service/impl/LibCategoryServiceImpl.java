package org.dwtech.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.dto.LibCategoryDto;
import org.dwtech.common.core.entity.po.LibCategoryPo;
import org.dwtech.common.utils.PageUtils;
import org.dwtech.mapper.LibCategoryMapper;
import org.dwtech.service.LibCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibCategoryServiceImpl implements LibCategoryService {
    private final LibCategoryMapper libCategoryMapper;

    public LibCategoryServiceImpl(LibCategoryMapper libCategoryMapper) {
        this.libCategoryMapper = libCategoryMapper;
    }

    @Override
    public IPage<LibCategoryDto> selectLibCategoryList(LibCategoryDto libCategoryDto) {
        LibCategoryPo libCategoryPo = new LibCategoryPo();
        BeanUtil.copyProperties(libCategoryDto, libCategoryPo);

        Page<LibCategoryDto> page = new Page<>();
        IPage<LibCategoryPo> libCategoryPoIPage = libCategoryMapper.selectLibCategoryList(new Page<>(PageUtils.getPageStart(), PageUtils.getPageSize()), libCategoryPo);
        BeanUtil.copyProperties(libCategoryPoIPage, page);

        List<LibCategoryDto> libCategoryDtoList = new ArrayList<>();
        libCategoryPoIPage.getRecords().forEach(item -> {
            LibCategoryDto libCategoryDto1 = new LibCategoryDto();
            BeanUtil.copyProperties(item, libCategoryDto1);
            libCategoryDtoList.add(libCategoryDto1);
        });
        page.setRecords(libCategoryDtoList);
        return page;
    }

    @Override
    public IPage<LibCategoryDto> buildCategoryTree(IPage<LibCategoryDto> categoryDtoIPage) {
        Page<LibCategoryDto> page = new Page<>();
        List<LibCategoryDto> categorys = categoryDtoIPage.getRecords();

        List<LibCategoryDto> treeCategorys = buildTree(categorys);

        page.setRecords(treeCategorys);
        page.setTotal(categoryDtoIPage.getTotal());
        page.setSize(categoryDtoIPage.getSize());
        page.setCurrent(categoryDtoIPage.getCurrent());
        return page;
    }

    private List<LibCategoryDto> buildTree(List<LibCategoryDto> categorys) {
        List<LibCategoryDto> treeList = new ArrayList<>();
        Map<Long, LibCategoryDto> categoryMap = new HashMap<>();

        for (LibCategoryDto libCategoryDto : categorys) {
            categoryMap.put(libCategoryDto.getCategoryId(), libCategoryDto);
        }

        for (LibCategoryDto libCategoryDto : categorys) {
            Long parentId = libCategoryDto.getParentId();
            if (parentId == 0) {
                treeList.add(libCategoryDto);
            } else {
                LibCategoryDto parentCategory = categoryMap.get(parentId);
                if (parentCategory != null) {
                    if (parentCategory.getChildren() == null) {
                        parentCategory.setChildren(new ArrayList<>());
                    }
                    parentCategory.getChildren().add(libCategoryDto);
                }
            }
        }

        return treeList;
    }
}
