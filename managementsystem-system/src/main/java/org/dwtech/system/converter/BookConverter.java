package org.dwtech.system.converter;

import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.BookForm;
import org.dwtech.common.core.entity.po.BookPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

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
