package org.dwtech.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.converter.StockConverter;
import org.dwtech.system.mapper.BookshelfMapper;
import org.dwtech.system.mapper.StockMapper;
import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.bo.StockAddResult;
import org.dwtech.system.model.entity.BookshelfPO;
import org.dwtech.system.model.entity.BookPO;
import org.dwtech.system.model.entity.StockPO;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.query.StockPageQuery;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.model.vo.StockPageVO;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.RecommendationService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final int PUBLIC_BOOK_RECOMMENDATION_LIMIT = 100;
    private static final int ENABLED = 1;

    private final StockConverter stockConverter;
    private final BookService bookService;
    private final RecommendationService recommendationService;
    private final BookshelfMapper bookshelfMapper;

    /**
     * 获取公开书目分页（集成协同过滤推荐）。
     * <p>
     * 三种场景：
     * <ol>
     *   <li>匿名用户 / 无推荐数据 → 原样返回默认排序</li>
     *   <li>已登录 + 关键词搜索 + 有推荐 → 关键词查询结果页内重排，推荐书置顶</li>
     *   <li>已登录 + 无关键词 + 有推荐 → 推荐书优先展示，常规馆藏填充剩余位</li>
     * </ol>
     */
    @Override
    public IPage<PublicBookPageVO> getPublicBookPage(PublicBookPageQuery queryParams) {
        log.info("获取公开书目分页：{}", queryParams);
        Page<StockBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<StockBO> stockPage = this.baseMapper.getPublicBookPage(page, queryParams);
        Long userId = SecurityUtils.getUserId();
        // 场景一：匿名用户，直接返回默认排序
        if (userId == null) {
            return stockConverter.toPublicPageVo(stockPage);
        }

        List<String> recommendedIsbns = recommendationService.getRecommendedIsbns(userId, PUBLIC_BOOK_RECOMMENDATION_LIMIT);
        // 场景一（续）：已登录但无借阅史，走默认排序
        if (recommendedIsbns.isEmpty()) {
            return stockConverter.toPublicPageVo(stockPage);
        }

        // 场景二：关键词搜索 + 有推荐 → 页内重排，推荐项置顶
        if (hasKeyword(queryParams)) {
            stockPage.setRecords(reorderByRecommendations(stockPage.getRecords(), recommendedIsbns));
            return stockConverter.toPublicPageVo(stockPage);
        }

        // 场景三：无关键词浏览 + 有推荐 → 推荐书优先 + 常规馆藏填充
        Page<StockBO> personalizedPage = buildPersonalizedPublicBookPage(queryParams, stockPage, recommendedIsbns);
        return stockConverter.toPublicPageVo(personalizedPage);
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
        StockPO existingStock = this.getById(stock.getIsbn());
        Long targetShelfId = resolveTargetShelfId(stockForm.getShelfId(), existingStock);
        validateShelfCapacityForStockAdd(existingStock, targetShelfId, stock.getStock());
        // 第一步：写入库存（upsert，原子累加）
        this.baseMapper.upsertStock(stock.getIsbn(), stock.getStock(), targetShelfId);
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

    @Override
    @Transactional
    public boolean updateStockShelf(String isbn, Long shelfId) {
        StockPO stock = this.getById(isbn);
        if (stock == null) {
            if (shelfId == null) {
                return true;
            }
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "库存记录不存在");
        }
        if (Objects.equals(stock.getShelfId(), shelfId)) {
            return true;
        }
        if (shelfId != null) {
            validateBookshelfCanHold(shelfId, safeStock(stock.getStock()));
        }
        return this.baseMapper.updateShelfId(isbn, shelfId) > 0;
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

    private Long resolveTargetShelfId(Long requestedShelfId, StockPO existingStock) {
        if (requestedShelfId != null) {
            return requestedShelfId;
        }
        return existingStock == null ? null : existingStock.getShelfId();
    }

    private void validateShelfCapacityForStockAdd(StockPO existingStock, Long targetShelfId, Integer amount) {
        if (targetShelfId == null) {
            return;
        }
        int incomingStock = safeStock(amount);
        Long existingShelfId = existingStock == null ? null : existingStock.getShelfId();
        int currentBookStock = existingStock == null ? 0 : safeStock(existingStock.getStock());
        int additionalStock = Objects.equals(existingShelfId, targetShelfId)
                ? incomingStock
                : currentBookStock + incomingStock;
        validateBookshelfCanHold(targetShelfId, additionalStock);
    }

    private void validateBookshelfCanHold(Long shelfId, int additionalStock) {
        BookshelfPO shelf = bookshelfMapper.selectById(shelfId);
        if (shelf == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "书架不存在");
        }
        if (!Objects.equals(shelf.getStatus(), ENABLED)) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "停用书架不能绑定图书");
        }
        int usedStock = safeStock(bookshelfMapper.sumStockByShelfId(shelfId));
        if (usedStock + additionalStock > safeStock(shelf.getCapacity())) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "书架容量不足");
        }
    }

    private static int safeStock(Integer stock) {
        return stock == null ? 0 : stock;
    }

    /**
     * 构建个性化分页结果。
     * <ol>
     *   <li>按推荐顺序查询图书完整信息</li>
     *   <li>计算当前页的推荐书窗口：[pageStart, pageStart+pageSize)</li>
     *   <li>不足 pageSize 条时，从常规馆藏中排除已展示的推荐书并填充</li>
     *   <li>总数沿用默认查询的 total（全馆藏总量），保证前端分页组件正常</li>
     * </ol>
     */
    private Page<StockBO> buildPersonalizedPublicBookPage(PublicBookPageQuery queryParams,
                                                          Page<StockBO> defaultPage,
                                                          List<String> recommendedIsbns) {
        int pageSize = queryParams.getPageSize();
        long pageStart = ((long) queryParams.getPageNum() - 1) * pageSize;
        List<StockBO> recommendedStocks = getOrderedRecommendationStocks(recommendedIsbns);
        if (recommendedStocks.isEmpty()) {
            return defaultPage;
        }

        List<StockBO> records = new ArrayList<>(pageSize);
        if (pageStart < recommendedStocks.size()) {
            int recommendationStart = (int) pageStart;
            int recommendationEnd = (int) Math.min(pageStart + pageSize, recommendedStocks.size());
            records.addAll(recommendedStocks.subList(recommendationStart, recommendationEnd));
        }

        int fillCount = pageSize - records.size();
        if (fillCount > 0) {
            Set<String> excludeIsbns = collectIsbns(recommendedStocks);
            long regularOffset = Math.max(0, pageStart - recommendedStocks.size());
            records.addAll(this.baseMapper.getPublicBookListExcluding(queryParams, excludeIsbns, regularOffset, fillCount));
        }

        Page<StockBO> personalizedPage = new Page<>(queryParams.getPageNum(), pageSize);
        personalizedPage.setTotal(defaultPage.getTotal());
        personalizedPage.setRecords(records);
        return personalizedPage;
    }

    private List<StockBO> getOrderedRecommendationStocks(List<String> recommendedIsbns) {
        List<StockBO> stocks = this.baseMapper.getStockByExacts(recommendedIsbns);
        Map<String, StockBO> stockMap = new HashMap<>(stocks.size());
        for (StockBO stock : stocks) {
            stockMap.putIfAbsent(stock.getIsbn(), stock);
        }

        List<StockBO> orderedStocks = new ArrayList<>(stocks.size());
        for (String isbn : recommendedIsbns) {
            StockBO stock = stockMap.get(isbn);
            if (stock != null) {
                orderedStocks.add(stock);
            }
        }
        return orderedStocks;
    }

    /** 页内重排：推荐书按推荐顺序置顶，其余保持原序 */
    private List<StockBO> reorderByRecommendations(List<StockBO> records, List<String> recommendedIsbns) {
        Map<String, Integer> recommendationOrder = new HashMap<>(recommendedIsbns.size());
        for (int i = 0; i < recommendedIsbns.size(); i++) {
            recommendationOrder.putIfAbsent(recommendedIsbns.get(i), i);
        }

        List<StockBO> recommendedRecords = records.stream()
                .filter(record -> recommendationOrder.containsKey(record.getIsbn()))
                .sorted((left, right) -> Integer.compare(
                        recommendationOrder.get(left.getIsbn()),
                        recommendationOrder.get(right.getIsbn())))
                .toList();
        List<StockBO> normalRecords = records.stream()
                .filter(record -> !recommendationOrder.containsKey(record.getIsbn()))
                .toList();

        List<StockBO> reorderedRecords = new ArrayList<>(records.size());
        reorderedRecords.addAll(recommendedRecords);
        reorderedRecords.addAll(normalRecords);
        return reorderedRecords;
    }

    private Set<String> collectIsbns(List<StockBO> stocks) {
        Set<String> isbns = new HashSet<>(stocks.size());
        for (StockBO stock : stocks) {
            isbns.add(stock.getIsbn());
        }
        return isbns;
    }

    private boolean hasKeyword(PublicBookPageQuery queryParams) {
        return StrUtil.isNotBlank(queryParams.getField()) && StrUtil.isNotBlank(queryParams.getKeyword());
    }
}
