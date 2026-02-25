package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.entity.BookPO;

import java.util.List;
/**
 * BookService
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface BookService extends IService<BookPO> {
    boolean saveOrUpdateBook(BookForm bookForm);

    BookForm getBookByIsbn(String isbn);

    List<Option<Long>> listBookOptions();
}
