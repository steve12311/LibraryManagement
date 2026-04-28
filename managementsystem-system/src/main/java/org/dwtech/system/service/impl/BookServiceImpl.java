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
 * 图书元数据服务实现，通过 BookConverter 完成 PO/Form 互转。
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, BookPO> implements BookService {
    private final BookConverter bookConverter;

    /**
     * 保存或更新图书。将 BookForm 通过 Converter 转为 PO，委托 MyBatis-Plus 的 saveOrUpdate。
     *
     * @param bookForm 图书表单
     * @return true 表示操作成功，false 表示失败
     */
    @Override
    public boolean saveOrUpdateBook(BookForm bookForm) {
        BookPO bookPo = bookConverter.toPo(bookForm);
        return this.saveOrUpdate(bookPo);
    }

    /**
     * 仅当 ISBN 对应的图书不存在时插入（幂等），使用 Mapper 的 insertIfAbsent 方法。
     *
     * @param book 图书实体
     * @return true 表示插入成功，false 表示已存在
     */
    @Override
    public boolean saveBookIfAbsent(BookPO book) {
        return this.baseMapper.insertIfAbsent(book) > 0;
    }

    /**
     * 根据 ISBN 查询图书，并通过 Converter 转为 Form 返回。
     *
     * @param isbn ISBN 编号
     * @return 图书表单，未找到时返回 null
     */
    @Override
    public BookForm getBookByIsbn(String isbn) {
        BookPO book = this.getById(isbn);
        return bookConverter.toForm(book);
    }

    /**
     * 查询全部图书的下拉选项列表，通过 Converter 将 PO 列表转为 Option 列表。
     *
     * @return 图书选项列表
     */
    @Override
    public List<Option<Long>> listBookOptions() {
        List<BookPO> list = this.list();
        return bookConverter.toOptions(list);
    }
}
