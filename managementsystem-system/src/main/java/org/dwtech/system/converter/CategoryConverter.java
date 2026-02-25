package org.dwtech.system.converter;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.CategoryPO;
import org.dwtech.system.model.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * CategoryConverter
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface CategoryConverter {
    @Mappings({
            @Mapping(source = "id", target = "categoryId"),
            @Mapping(source = "name", target = "categoryName"),
            @Mapping(source = "type", target = "code")
    })
    /**
     * 用途：转换为 vo。
     * 
     * @param po po
     * @return 返回结果
     */
    CategoryVO toVo(CategoryPO po);

    @Mappings({
            @Mapping(source = "id", target = "value"),
            @Mapping(source = "name", target = "label")
    })
    /**
     * 用途：转换为 option。
     * 
     * @param po po
     * @return 返回结果
     */
    Option<String> toOption(CategoryPO po);

    /**
     * 用途：转换为 options。
     * 
     * @param list list
     * @return 结果列表
     */
    List<Option<String>> toOptions(List<CategoryPO> list);
}
