package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.BookForm;
import org.dwtech.common.core.entity.po.BookPO;

import java.util.List;

public interface BookService extends IService<BookPO> {
    boolean saveOrUpdateBook(BookForm bookForm);

    BookForm getBookByIsbn(String isbn);

    List<Option<Long>> listBookOptions();
}
