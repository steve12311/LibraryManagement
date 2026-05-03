package org.dwtech.system.converter;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.CategoryPO;
import org.dwtech.system.model.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * 分类对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface CategoryConverter {
    @Mappings({
            @Mapping(source = "id", target = "categoryId"),
            @Mapping(source = "name", target = "categoryName"),
            @Mapping(source = "type", target = "code"),
            @Mapping(target = "hasChildren", ignore = true)
    })
    /**
     * CategoryPO → CategoryVO
     */
    CategoryVO toVo(CategoryPO po);

    @Mappings({
            @Mapping(source = "id", target = "value"),
            @Mapping(source = "name", target = "label")
    })
    /**
     * CategoryPO → Option
     */
    Option<String> toOption(CategoryPO po);

    /**
     * List<CategoryPO> → List<Option>
     */
    List<Option<String>> toOptions(List<CategoryPO> list);
}
