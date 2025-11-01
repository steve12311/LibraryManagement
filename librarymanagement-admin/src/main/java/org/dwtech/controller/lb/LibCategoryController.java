package org.dwtech.controller.lb;

import cn.hutool.core.bean.BeanUtil;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.LibCategoryDto;
import org.dwtech.common.core.entity.vo.LibCategoryVo;
import org.dwtech.service.LibCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/lib/category")
public class LibCategoryController {
    private final LibCategoryService libCategoryService;

    public LibCategoryController(LibCategoryService libCategoryService) {
        this.libCategoryService = libCategoryService;
    }

    @GetMapping("/list")
    public AjaxResult getLibCategory(LibCategoryDto libCategoryDto) {
        List<LibCategoryDto> libCategoryDtoTree = libCategoryService.buildCategoryTree(libCategoryService.selectLibCategoryList(libCategoryDto));
        List<LibCategoryVo> libCategoryTree = new ArrayList<>();
        libCategoryDtoTree.forEach(item -> {
            libCategoryTree.add(convertToVo(item));
        });
        return AjaxResult.success(libCategoryTree);
    }

    /**
     * 使用Hutool进行DTO转VO
     */
    private LibCategoryVo convertToVo(LibCategoryDto dto) {
        // 使用Hutool的BeanUtil进行属性拷贝
        return BeanUtil.copyProperties(dto, LibCategoryVo.class);
    }

}
