package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.common.service.MilvusService;
import org.dwtech.system.converter.BookConverter;
import org.dwtech.system.mapper.BookMapper;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, BookPO> implements BookService {
    private final BookConverter bookConverter;

    @Override
    public boolean saveOrUpdateBook(BookForm bookForm) {
        BookPO bookPo = bookConverter.toPo(bookForm);
        return this.saveOrUpdate(bookPo);
    }

    @Override
    public BookForm getBookByIsbn(String isbn) {
        BookPO book = this.getById(isbn);
        return bookConverter.toForm(book);
    }

    @Override
    public List<Option<Long>> listBookOptions() {
        List<BookPO> list = this.list();
        return bookConverter.toOptions(list);
    }
}
