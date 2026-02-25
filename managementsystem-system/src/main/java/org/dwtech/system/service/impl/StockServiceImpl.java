package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.system.converter.StockConverter;
import org.dwtech.system.mapper.StockMapper;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * StockServiceImpl
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
    public IPage<StockPageVO> getStockPage(StockPageQuery queryParams) {
        log.info("获取书籍分页：{}", queryParams);
        // 参数构建
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<StockBO> page = new Page<>(pageNum, pageSize);

        Page<StockBO> stockPage = this.baseMapper.getStockPage(page, queryParams);

        return stockConverter.toPageVo(stockPage);
    }

    @Override
    public List<StockPageVO> getStockByExacts(List<String> isbns) {
        log.info("查询书籍信息：{}", isbns);
        List<StockBO> stockList = this.baseMapper.getStockByExacts(isbns);
        return stockConverter.toListVo(stockList);
    }

    @Override
    @Transactional
    public boolean addStock(StockForm stockForm) {
        log.info("书籍入库开始：{}", stockForm);
        StockBO stockBo = stockConverter.toBo(stockForm);

        StockPO stock = stockConverter.toPo(stockBo);
        StockPO nowStock = this.getById(stock.getIsbn());
        if (nowStock != null) {
            stock.setCurrentStock(nowStock.getCurrentStock() + stock.getStock());
            stock.setStock(nowStock.getStock() + stock.getStock());
        } else {
            stock.setCurrentStock(stock.getStock());
        }
        this.saveOrUpdate(stock);
        log.info("书籍入库步骤一完成：{}", stock);

        if (nowStock == null) {
            BookPO book = stockConverter.toBookPo(stockBo);
            bookService.saveOrUpdate(book);
            log.info("书籍入库步骤二完成：{}", book);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean outStock(StockForm stockForm) {
        log.info("书籍出库开始：{}", stockForm);
        StockPO stockPo = stockConverter.toPo(stockForm);
        // 出库数量不能大于当前剩余数量
        StockPO nowStock = this.getById(stockPo.getIsbn());
        if (stockPo.getStock() > nowStock.getCurrentStock()) {
            throw new RuntimeException("出库数量不能大于当前剩余数量");
        }

        nowStock.setStock(nowStock.getStock() - stockPo.getStock());
        nowStock.setCurrentStock(nowStock.getCurrentStock() - stockPo.getStock());
        this.saveOrUpdate(nowStock);

        log.info("书籍出库完成：{}", stockPo);
        return true;
    }

    @Override
    @Transactional
    public boolean borrowOut(StockForm stockForm) {
        log.info("书籍出借库存开始：{}", stockForm);
        StockPO stockPo = stockConverter.toPo(stockForm);
        StockPO nowStock = this.getById(stockPo.getIsbn());
        if (stockPo.getStock() > nowStock.getCurrentStock()) {
            throw new RuntimeException("书籍数量不足");
        }

        nowStock.setCurrentStock(nowStock.getCurrentStock() - stockPo.getStock());
        this.saveOrUpdate(nowStock);

        log.info("书籍出借出库完成：{}", stockPo);
        return true;
    }

    @Override
    public boolean borrowEnter(StockForm stockForm) {
        log.info("书籍还书库存开始：{}", stockForm);
        StockPO stockPo = stockConverter.toPo(stockForm);
        StockPO nowStock = this.getById(stockPo.getIsbn());
        nowStock.setCurrentStock(nowStock.getCurrentStock() + stockPo.getStock());

        this.saveOrUpdate(nowStock);
        log.info("书籍还书入库完成：{}", stockPo);
        return true;
    }

    @Override
    public StockForm getStockFormData(String isbn) {
        StockBO stock = this.baseMapper.selectStockById(isbn);
        return stockConverter.toForm(stock);
    }
}
