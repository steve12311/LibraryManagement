package org.dwtech.system.converter;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.entity.BookPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * BookConverter
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface BookConverter {
    /**
     * 用途：转换为 po。
     * 
     * @param bookForm book form
     * @return 返回结果
     */
    BookPO toPo(BookForm bookForm);

    /**
     * 用途：转换为 form。
     * 
     * @param bookPO book po
     * @return 返回结果
     */
    BookForm toForm(BookPO bookPO);

    @Mappings({
            @Mapping(target = "label", source = "isbn"),
            @Mapping(target = "value", source = "isbn"),
    })
    /**
     * 用途：转换为 option。
     * 
     * @param po po
     * @return 返回结果
     */
    Option<Long> toOption(BookPO po);

    /**
     * 用途：转换为 options。
     * 
     * @param list list
     * @return 结果列表
     */
    List<Option<Long>> toOptions(List<BookPO> list);
}
