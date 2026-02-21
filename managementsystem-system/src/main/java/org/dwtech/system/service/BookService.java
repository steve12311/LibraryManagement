package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.core.entity.form.BookForm;
import org.dwtech.common.core.entity.po.BookPO;

public interface BookService extends IService<BookPO> {
    boolean updateBook(BookForm bookForm);

    BookForm getBookByIsbn(String isbn);
}
