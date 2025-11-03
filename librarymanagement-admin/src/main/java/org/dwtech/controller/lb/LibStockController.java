package org.dwtech.controller.lb;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.LibBookStockDto;
import org.dwtech.common.core.entity.dto.LibCategoryDto;
import org.dwtech.common.core.entity.dto.LibPublishDto;
import org.dwtech.common.core.entity.vo.LibBookStockVo;
import org.dwtech.service.LibCategoryService;
import org.dwtech.service.LibPublishService;
import org.dwtech.service.LibStockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lib/stock")
public class LibStockController extends BaseController {
    private final LibStockService libStockService;
    private final LibPublishService libPublishService;
    private final LibCategoryService libCategoryService;

    public LibStockController(LibStockService libStockService, LibPublishService libPublishService, LibCategoryService libCategoryService) {
        this.libStockService = libStockService;
        this.libPublishService = libPublishService;
        this.libCategoryService = libCategoryService;
    }

    @GetMapping("/list")
    public AjaxResult getBookStock(LibBookStockDto libBookStockDto) {
        Page<LibBookStockVo> page = new Page<>();
        IPage<LibBookStockDto> libBookStockDtoList = libStockService.selectStockList(libBookStockDto);
        BeanUtil.copyProperties(libBookStockDtoList, page);

        Long[] publishIds = libBookStockDtoList.getRecords().stream()
                .map(LibBookStockDto::getPublishId)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(Long[]::new);
        Long[] categoryIds = libBookStockDtoList.getRecords().stream()
                .map(LibBookStockDto::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(Long[]::new);

        List<LibCategoryDto> libCategoryDtos = libCategoryService.selectLibCategoryByIds(categoryIds);
        List<LibPublishDto> libPublishDtos = libPublishService.selectPublishByIds(publishIds);

        Map<Long, String> publishMap = libPublishDtos.stream()
                .collect(Collectors.toMap(
                        LibPublishDto::getPublishId,
                        LibPublishDto::getPublishName
                ));
        Map<Long, String> categoryMap = libCategoryDtos.stream()
                .collect(Collectors.toMap(
                        LibCategoryDto::getCategoryId,
                        LibCategoryDto::getCategoryName
                ));

        List<LibBookStockVo> bookStockVoList = new ArrayList<>();
        libBookStockDtoList.getRecords().forEach(item -> {
            LibBookStockVo libBookStockVo = new LibBookStockVo();
            BeanUtil.copyProperties(item, libBookStockVo);
            libBookStockVo.setPublishName(publishMap.get(item.getPublishId()));
            libBookStockVo.setCategoryName(categoryMap.get(item.getCategoryId()));
            bookStockVoList.add(libBookStockVo);
        });
        page.setRecords(bookStockVoList);
        return AjaxResult.success(page);
    }
}
