package org.dwtech.system.converter;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.entity.BookPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * 图书对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper(componentModel = "spring")
public interface BookConverter {
    /**
     * BookForm → BookPO
     */
    BookPO toPo(BookForm bookForm);

    /**
     * BookPO → BookForm
     */
    BookForm toForm(BookPO bookPO);

    @Mappings({
            @Mapping(target = "label", source = "isbn"),
            @Mapping(target = "value", source = "isbn"),
    })
    /**
     * BookPO → Option
     */
    Option<Long> toOption(BookPO po);

    /**
     * List<BookPO> → List<Option>
     */
    List<Option<Long>> toOptions(List<BookPO> list);
}
