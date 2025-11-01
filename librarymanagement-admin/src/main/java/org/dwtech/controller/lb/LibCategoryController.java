package org.dwtech.controller.lb;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.LibCategoryDto;
import org.dwtech.common.core.entity.vo.LibCategoryVo;
import org.dwtech.service.LibCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        IPage<LibCategoryDto> page = libCategoryService.buildCategoryTree(libCategoryService.selectLibCategoryList(libCategoryDto));
        Page<LibCategoryVo> pageVo = new Page<>();
        BeanUtil.copyProperties(page, pageVo);

        List<LibCategoryVo> libCategoryVoList = page.getRecords().stream()
                .map(this::convertCategoryDtoToVo)
                .toList();
        pageVo.setRecords(libCategoryVoList);
        return AjaxResult.success(pageVo);
    }

    private LibCategoryVo convertCategoryDtoToVo(LibCategoryDto libCategoryDto) {
        LibCategoryVo libCategoryVo = new LibCategoryVo();
        BeanUtil.copyProperties(libCategoryDto, libCategoryVo);

        List<LibCategoryDto> children = libCategoryDto.getChildren();
        if (children != null && !children.isEmpty()) {
            List<LibCategoryVo> childrenVoList = children.stream()
                    .map(this::convertCategoryDtoToVo)
                    .toList();
            libCategoryVo.setChildren(childrenVoList);
        } else {
            libCategoryVo.setChildren(null);
        }
        return libCategoryVo;
    }
}
