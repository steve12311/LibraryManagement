package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.model.Option;
import org.dwtech.system.converter.BookConverter;
import org.dwtech.system.mapper.BookMapper;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * BookServiceImpl
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, BookPO> implements BookService {
    private final BookConverter bookConverter;

    /**
     * 用途：保存 or update book。
     * 
     * @param bookForm book form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    public boolean saveOrUpdateBook(BookForm bookForm) {
        BookPO bookPo = bookConverter.toPo(bookForm);
        return this.saveOrUpdate(bookPo);
    }

    /**
     * 用途：仅当图书不存在时保存图书元数据。
     *
     * @param book 图书实体
     * @return 操作结果，true 表示插入成功，false 表示已存在
     */
    @Override
    public boolean saveBookIfAbsent(BookPO book) {
        return this.baseMapper.insertIfAbsent(book) > 0;
    }

    /**
     * 用途：获取 book by isbn 信息。
     * 
     * @param isbn isbn
     * @return 返回结果
     */
    @Override
    public BookForm getBookByIsbn(String isbn) {
        BookPO book = this.getById(isbn);
        return bookConverter.toForm(book);
    }

    /**
     * 用途：查询 book options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    @Override
    public List<Option<Long>> listBookOptions() {
        List<BookPO> list = this.list();
        return bookConverter.toOptions(list);
    }
}
