package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.query.StockPageQuery;

import java.util.List;
import java.util.Set;
/**
 * 库存数据访问层，提供图书库存的分页、查询和变更接口
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Mapper
public interface StockMapper extends BaseMapper<StockPO> {
    /**
     * 分页查询公开书目列表，供读者浏览
     *
     * @return 分页结果
     */
    Page<StockBO> getPublicBookPage(Page<StockBO> page, @Param("queryParams") PublicBookPageQuery queryParams);

    /**
     * 按偏移量查询公开书目列表，并排除指定 ISBN。
     *
     * @return 公开书目列表
     */
    List<StockBO> getPublicBookListExcluding(@Param("queryParams") PublicBookPageQuery queryParams,
                                             @Param("excludeIsbns") Set<String> excludeIsbns,
                                             @Param("offset") long offset,
                                             @Param("limit") int limit);

    /**
     * 分页查询库存列表，支持多种筛选条件
     *
     * @return 分页结果
     */
    Page<StockBO> getStockPage(Page<StockBO> page, @Param("queryParams") StockPageQuery queryParams);

    /**
     * 根据 ISBN 列表批量查询库存信息
     *
     * @return 库存列表
     */
    List<StockBO> getStockByExacts(@Param("isbns") List<String> isbns);

    /**
     * 根据 ISBN 查询单个库存信息
     *
     * @return 库存信息
     */
    StockBO selectStockById(String isbn);

    /**
     * 对已存在库存执行原子入库累加，同时增加总库存和可用库存
     *
     * @param amount 变更数量
     * @return 影响行数
     */
    int increaseStockAndCurrentStock(@Param("isbn") String isbn, @Param("amount") Integer amount);

    /**
     * 执行带库存校验的原子出库，同时减少总库存和可用库存
     *
     * @param amount 变更数量
     * @return 影响行数
     */
    int decreaseStockAndCurrentStock(@Param("isbn") String isbn, @Param("amount") Integer amount);

    /**
     * 执行带剩余库存校验的原子借出，仅减少可用库存
     *
     * @param amount 变更数量
     * @return 影响行数
     */
    int decreaseCurrentStock(@Param("isbn") String isbn, @Param("amount") Integer amount);

    /**
     * 执行原子还书入库，仅增加可用库存
     *
     * @param amount 变更数量
     * @return 影响行数
     */
    int increaseCurrentStock(@Param("isbn") String isbn, @Param("amount") Integer amount);

    /**
     * 执行库存首次入库或已存在库存的原子 upsert
     *
     * @param amount 变更数量
     * @return 影响行数
     */
    int upsertStock(@Param("isbn") String isbn, @Param("amount") Integer amount, @Param("shelfId") Long shelfId);

    /**
     * 更新指定 ISBN 的书架绑定。
     *
     * @param isbn    ISBN
     * @param shelfId 书架 ID，空值表示清空绑定
     * @return 影响行数
     */
    int updateShelfId(@Param("isbn") String isbn, @Param("shelfId") Long shelfId);
}
