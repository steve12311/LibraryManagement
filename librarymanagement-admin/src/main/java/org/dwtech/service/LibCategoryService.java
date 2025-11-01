package org.dwtech.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dwtech.common.core.entity.dto.LibCategoryDto;

public interface LibCategoryService {
    IPage<LibCategoryDto> selectLibCategoryList(LibCategoryDto libCategoryDto);
    IPage<LibCategoryDto> buildCategoryTree(IPage<LibCategoryDto> categoryDtoIPage);
}
