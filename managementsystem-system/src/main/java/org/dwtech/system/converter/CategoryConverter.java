package org.dwtech.system.converter;

import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.po.CategoryPO;
import org.dwtech.common.core.entity.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryConverter {
    @Mappings({
            @Mapping(source = "id", target = "categoryId"),
            @Mapping(source = "name", target = "categoryName"),
            @Mapping(source = "type", target = "code")
    })
    CategoryVO toVo(CategoryPO po);

    @Mappings({
            @Mapping(source = "id", target = "value"),
            @Mapping(source = "name", target = "label")
    })
    Option<String> toOption(CategoryPO po);

    List<Option<String>> toOptions(List<CategoryPO> list);
}
