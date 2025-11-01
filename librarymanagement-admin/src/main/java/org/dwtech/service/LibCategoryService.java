package org.dwtech.service;

import org.dwtech.common.core.entity.dto.LibCategoryDto;

import java.util.List;

public interface LibCategoryService {
    List<LibCategoryDto> selectLibCategoryList(LibCategoryDto libCategoryDto);
    List<LibCategoryDto> buildCategoryTree(List<LibCategoryDto> categoryDtoList);
}
