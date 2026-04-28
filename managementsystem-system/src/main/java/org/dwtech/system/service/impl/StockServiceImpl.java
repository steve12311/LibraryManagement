package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.StockConverter;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.system.mapper.StockMapper;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 库存管理服务实现
 * <p>
 * 库存操作分为两大类：
 * <ul>
 *   <li>管理端操作：入库（{@link #addStock} 调用 {@code upsertStock}，不存在则插入、存在则累加）、出库（{@link #outStock} 扣减总库存+当前库存）</li>
 *   <li>借阅驱动操作：借出（{@link #borrowOut} 仅扣当前可借库存）、归还（{@link #borrowEnter} 恢复当前可借库存）</li>
 * </ul>
 * 所有库存变更通过 MyBatis XML 中的原子 SQL（{@code stock = stock - #{stock}}）保证并发安全，
 * 更新失败后回查库存状态抛出明确的业务异常。
 *
 * @author steve12311
 * @since 2026-02-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, StockPO> implements StockService {
    private final StockConverter stockConverter;
    private final BookService bookService;

    @Override
    public IPage<PublicBookPageVO> getPublicBookPage(PublicBookPageQuery queryParams) {
        log.info("获取公开书目分页：{}", queryParams);
        Page<StockBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<StockBO> stockPage = this.baseMapper.getPublicBookPage(page, queryParams);
        return stockConverter.toPublicPageVo(stockPage);
    }

    @Override
    public IPage<StockPageVO> getStockPage(StockPageQuery queryParams) {
        log.info("获取书籍分页：{}", queryParams);
        Page<StockBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<StockBO> stockPage = this.baseMapper.getStockPage(page, queryParams);
        return stockConverter.toPageVo(stockPage);
    }

    @Override
    public List<StockPageVO> getStockByExacts(List<String> isbns) {
        log.info("查询书籍信息：{}", isbns);
        List<StockBO> stockList = this.baseMapper.getStockByExacts(isbns);
        return stockConverter.toListVo(stockList);
    }

    /**
     * 图书入库
     * <p>
     * 流程：
     * <ol>
     *   <li>调用 {@code upsertStock} — 不存在则插入库存记录，存在则累加总库存和当前库存（原子操作）</li>
     *   <li>调用 {@link BookService#saveBookIfAbsent} — 仅首次入库时写入图书主数据</li>
     * </ol>
     *
     * @return {@code StockAddResult}，其中 {@code firstStockIngested} 表示是否为首次入库（触发图书主数据写入）
     */
    @Override
    @Transactional
    public StockAddResult addStock(StockForm stockForm) {
        log.info("书籍入库开始：{}", stockForm);
        StockBO stockBo = stockConverter.toBo(stockForm);
        StockPO stock = stockConverter.toPo(stockBo);
        // 第一步：写入库存（upsert，原子累加）
        this.baseMapper.upsertStock(stock.getIsbn(), stock.getStock());
        log.info("书籍入库步骤一完成：{}", stock);
        // 第二步：仅首次入库时写入图书主数据
        BookPO book = stockConverter.toBookPo(stockBo);
        boolean firstStockIngested = bookService.saveBookIfAbsent(book);
        log.info("书籍入库步骤二完成：{}", book);
        return new StockAddResult(true, firstStockIngested);
    }

    /**
     * 图书出库（管理端）
     * <p>
     * 同时扣减总库存和当前可借库存，通过原子 SQL {@code stock = stock - #{stock} AND current_stock >= #{stock}} 保证不超扣。
     */
    @Override
    @Transactional
    public boolean outStock(StockForm stockForm) {
        log.info("书籍出库开始：{}", stockForm);
        StockPO stockPo = stockConverter.toPo(stockForm);
        int updated = this.baseMapper.decreaseStockAndCurrentStock(stockPo.getIsbn(), stockPo.getStock());
        if (updated == 0) {
            throwStockMutationException(stockPo.getIsbn(), "出库数量不能大于当前剩余数量");
        }
        log.info("书籍出库完成：{}", stockPo);
        return true;
    }

    /**
     * 借阅出库 — 仅扣减当前可借库存
     * <p>
     * 与 {@link #outStock} 的区别：只扣 {@code current_stock}，不扣总库存。
     * 使用悲观锁（{@code SELECT ... FOR UPDATE}）防止超借。
     */
    @Override
    @Transactional
    public boolean borrowOut(StockForm stockForm) {
        log.info("书籍出借库存开始：{}", stockForm);
        StockPO stockPo = stockConverter.toPo(stockForm);
        int updated = this.baseMapper.decreaseCurrentStock(stockPo.getIsbn(), stockPo.getStock());
        if (updated == 0) {
            throwStockMutationException(stockPo.getIsbn(), "书籍数量不足");
        }
        log.info("书籍出借出库完成：{}", stockPo);
        return true;
    }

    /**
     * 还书入库 — 恢复当前可借库存
     * <p>
     * 原子增加 {@code current_stock}，仅当库存记录存在时成功。
     */
    @Override
    @Transactional
    public boolean borrowEnter(StockForm stockForm) {
        log.info("书籍还书库存开始：{}", stockForm);
        StockPO stockPo = stockConverter.toPo(stockForm);
        int updated = this.baseMapper.increaseCurrentStock(stockPo.getIsbn(), stockPo.getStock());
        if (updated == 0) {
            throwStockMutationException(stockPo.getIsbn(), "库存记录不存在");
        }
        log.info("书籍还书入库完成：{}", stockPo);
        return true;
    }

    @Override
    public StockForm getStockFormData(String isbn) {
        StockBO stock = this.baseMapper.selectStockById(isbn);
        return stockConverter.toForm(stock);
    }

    /**
     * 原子更新失败后的异常诊断：
     * 回查库存记录是否存在，区分"库存记录不存在"和"数量不足"两种错误，返回明确的业务异常。
     */
    private void throwStockMutationException(String isbn, String insufficientMessage) {
        StockPO currentStock = this.getById(isbn);
        if (currentStock == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "库存记录不存在");
        }
        throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, insufficientMessage);
    }
}
