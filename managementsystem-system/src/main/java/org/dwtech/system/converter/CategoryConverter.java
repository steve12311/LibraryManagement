package org.dwtech.system.converter;

import org.dwtech.common.core.entity.po.CategoryPO;
import org.dwtech.common.core.entity.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CategoryConverter {
    @Mappings({
            @Mapping(source = "id", target = "categoryId"),
            @Mapping(source = "name", target = "categoryName"),
            @Mapping(source = "type", target = "code")
    })
    CategoryVO toVo(CategoryPO po);
}
