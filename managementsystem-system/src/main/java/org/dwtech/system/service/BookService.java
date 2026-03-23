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
    /**
     * 用途：保存 or update book。
     * 
     * @param bookForm book form
     * @return 操作结果，true 表示成功，false 表示失败
     */
    boolean saveOrUpdateBook(BookForm bookForm);

    /**
     * 用途：仅当图书不存在时保存图书元数据。
     *
     * @param book 图书实体
     * @return 操作结果，true 表示插入成功，false 表示已存在
     */
    boolean saveBookIfAbsent(BookPO book);

    /**
     * 用途：获取 book by isbn 信息。
     * 
     * @param isbn isbn
     * @return 返回结果
     */
    BookForm getBookByIsbn(String isbn);

    /**
     * 用途：查询 book options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<Option<Long>> listBookOptions();
}
