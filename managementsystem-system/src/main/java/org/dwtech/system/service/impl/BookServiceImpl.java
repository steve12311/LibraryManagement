package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.form.BookForm;
import org.dwtech.system.converter.BookConverter;
import org.dwtech.system.mapper.BookMapper;
import org.dwtech.common.core.entity.po.BookPO;
import org.dwtech.system.service.BookService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, BookPO> implements BookService {
    private final BookConverter bookConverter;

    @Override
    public boolean updateBook(BookForm bookForm) {
        BookPO bookPo = bookConverter.toPo(bookForm);
        return this.updateById(bookPo);
    }

    @Override
    public BookForm getBookByIsbn(String isbn) {
        BookPO book = this.baseMapper.selectById(isbn);
        return bookConverter.toForm(book);
    }
}
