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
    BookPO toPo(BookForm bookForm);

    BookForm toForm(BookPO bookPO);

    @Mappings({
            @Mapping(target = "label", source = "isbn"),
            @Mapping(target = "value", source = "isbn"),
    })
    Option<Long> toOption(BookPO po);

    List<Option<Long>> toOptions(List<BookPO> list);
}
