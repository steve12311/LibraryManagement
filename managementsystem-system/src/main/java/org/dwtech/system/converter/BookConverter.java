package org.dwtech.system.converter;

import org.dwtech.common.core.entity.form.BookForm;
import org.dwtech.common.core.entity.po.BookPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookConverter {
    BookPO toPo(BookForm bookForm);

    BookForm toForm(BookPO bookPO);
}
