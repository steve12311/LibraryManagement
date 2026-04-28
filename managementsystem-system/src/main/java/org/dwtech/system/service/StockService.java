package org.dwtech.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.model.vo.StockPageVO;

import java.util.List;
/**
 * 库存管理服务，提供书目展示、库存分页查询、入库、出库、借出和归还操作。
 *
 * @author steve12311
 * @since 2026-02-22
 */

public interface StockService {
    /**
     * 分页查询公开书目信息（不要求登录，用于大厅展示）。
     *
     * @param queryParams 分页查询参数
     * @return 公开书目分页结果
     */
    IPage<PublicBookPageVO> getPublicBookPage(@Valid PublicBookPageQuery queryParams);

    /**
     * 分页查询库存列表。
     *
     * @param queryParams 分页查询参数（页码、每页条数、筛选条件）
     * @return 库存分页结果
     */
    IPage<StockPageVO> getStockPage(@Valid StockPageQuery queryParams);

    /**
     * 根据 ISBN 列表批量查询库存信息（精确匹配）。
     *
     * @param isbns ISBN 编号列表
     * @return 库存信息列表
     */
    List<StockPageVO> getStockByExacts(List<String> isbns);

    /**
     * 新增入库。创建或关联图书元数据，增加馆藏数量。
     *
     * @param stockForm 入库表单（ISBN、书名、数量等信息）
     * @return 入库结果（含新增库存 ID）
     */
    StockAddResult addStock(@Valid StockForm stockForm);

    /**
     * 出库操作。减少馆藏数量，仅用于非借阅场景的物理出库。
     *
     * @param stockForm 出库表单（ISBN、数量等）
     * @return true 表示出库成功，false 表示失败
     */
    boolean outStock(@Valid StockForm stockForm);

    /**
     * 借出操作。扣减可用库存并记录借出数量（含乐观锁控制）。
     *
     * @param stockForm 借出表单
     * @return true 表示借出成功，false 表示失败
     */
    boolean borrowOut(StockForm stockForm);

    /**
     * 归还操作。增加可用库存并扣减退回数量。
     *
     * @param stockForm 归还表单
     * @return true 表示归还成功，false 表示失败
     */
    boolean borrowEnter(StockForm stockForm);

    /**
     * 根据 ISBN 查询库存表单数据（用于编辑回显）。
     *
     * @param isbn ISBN 编号
     * @return 库存表单
     */
    StockForm getStockFormData(String isbn);
}
