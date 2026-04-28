package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.entity.BookPO;

import java.util.List;
/**
 * 图书元数据服务，负责图书基本信息的增删查改、ISBN 检索及图书下拉选项。
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface BookService extends IService<BookPO> {
    /**
     * 保存或更新图书信息。根据 BookForm 是否存在 ID 自动判断新增或更新。
     *
     * @param bookForm 图书表单，包含 ISBN、书名、作者、出版社、封面等信息
     * @return true 表示操作成功，false 表示操作失败
     */
    boolean saveOrUpdateBook(BookForm bookForm);

    /**
     * 仅当图书不存在时保存图书元数据（幂等写入）。通常用于扫码入库等场景。
     *
     * @param book 图书实体
     * @return true 表示插入成功，false 表示已存在
     */
    boolean saveBookIfAbsent(BookPO book);

    /**
     * 根据 ISBN 查询图书详情。
     *
     * @param isbn ISBN 编号
     * @return 图书表单，未找到时返回 null
     */
    BookForm getBookByIsbn(String isbn);

    /**
     * 查询所有图书的下拉选项列表，供前端下拉选择器使用。
     *
     * @return 图书选项列表，每项包含图书 ID 和名称
     */
    List<Option<Long>> listBookOptions();
}
