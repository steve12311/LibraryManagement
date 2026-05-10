package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.form.BookForm;
/**
 * BookMapper
 *
 * @author steve12311
 * @since 2025-10-30
 */

@Mapper
public interface BookMapper extends BaseMapper<BookPO> {
    /**
     * 用途：仅当图书不存在时保存图书元数据。
     *
     * @param book 图书实体
     * @return 影响行数
     */
    int insertIfAbsent(@Param("book") BookPO book);

    /**
     * 查询图书表单数据，同时带出库存书架绑定。
     *
     * @param isbn ISBN
     * @return 图书表单
     */
    BookForm selectBookFormByIsbn(@Param("isbn") String isbn);
}
