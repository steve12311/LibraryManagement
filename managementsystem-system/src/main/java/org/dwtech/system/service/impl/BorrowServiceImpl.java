package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.uuid.UUID;
import org.dwtech.system.model.bo.BorrowBO;
import org.dwtech.system.model.bo.MyBorrowBO;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.query.BorrowPageQuery;
import org.dwtech.system.model.query.MyBorrowPageQuery;
import org.dwtech.system.model.vo.BorrowVO;
import org.dwtech.system.model.vo.MyBorrowPageVO;
import org.dwtech.system.converter.BorrowConverter;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.RecommendationService;
import org.dwtech.system.service.BorrowNotificationService;
import org.dwtech.system.service.ReservationService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * 借阅管理服务实现
 * <p>
 * 核心业务流程：
 * <ol>
 *   <li>借书（{@link #saveBorrow}）：校验用户 → 查图书 → 创建借阅记录 → 扣减可借库存（悲观锁）</li>
 *   <li>还书（{@link #updateBorrow}）：查借阅记录 → 校验未还 → 更新归还时间 → 恢复可借库存</li>
 *   <li>查询：分页查询借阅记录（管理员视角）和我的借阅（用户视角）</li>
 * </ol>
 *
 * @author steve12311
 * @since 2026-02-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, BorrowPO> implements BorrowService {
    private final BorrowConverter borrowConverter;
    private final BookService bookService;
    private final StockService stockService;
    private final RecommendationService recommendationService;
    private final ReservationService reservationService;
    private final BorrowNotificationService borrowNotificationService;

    @Override
    public IPage<BorrowVO> getBorrowPage(BorrowPageQuery queryParams) {
        Page<BorrowBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<BorrowBO> borrowPage = this.baseMapper.getBorrowPage(page, queryParams);
        return borrowConverter.toPageVo(borrowPage);
    }

    @Override
    public IPage<MyBorrowPageVO> getCurrentUserBorrowPage(Long userId, MyBorrowPageQuery queryParams) {
        Page<MyBorrowBO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<MyBorrowBO> borrowPage = this.baseMapper.getCurrentUserBorrowPage(page, userId, queryParams);
        return borrowConverter.toMyBorrowPageVo(borrowPage);
    }

    /**
     * 创建借阅记录（借书）
     * <p>
     * 流程：
     * <ol>
     *   <li>校验借书用户 ID 不为空（馆员代借场景）</li>
     *   <li>根据 ISBN 查询图书信息，不存在则抛异常</li>
     *   <li>写入借阅记录（UUID 主键，避免分布式自增冲突）</li>
     *   <li>调用库存服务扣减可借数量（{@link StockService#borrowOut}，悲观锁保证并发安全）</li>
     * </ol>
     */
    @Override
    @Transactional
    public boolean saveBorrow(BorrowForm formData) {
        validateBorrowUserId(formData);
        String uuid = UUID.randomUUID().toString();
        BookForm bookForm = bookService.getBookByIsbn(formData.getIsbn());
        if (bookForm == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "图书不存在");
        }
        String bookName = bookForm.getName();

        BorrowPO borrow = borrowConverter.toPo(formData);
        borrow.setBookName(bookName);
        borrow.setId(uuid);
        this.saveOrUpdate(borrow);

        StockForm stockForm = new StockForm();
        stockForm.setIsbn(formData.getIsbn());
        stockForm.setStock(1);
        stockService.borrowOut(stockForm);
        recommendationService.invalidateUserCache(formData.getUserId());
        return true;
    }

    /**
     * 归还借阅（还书）
     * <p>
     * 流程：
     * <ol>
     *   <li>查询借阅记录，不存在则抛异常</li>
     *   <li>校验是否已还书（{@code realityReturnTime != null} 即表示已还）</li>
     *   <li>更新借阅记录（含归还时间）</li>
     *   <li>若设置了归还时间，调用库存服务恢复可借数量（{@link StockService#borrowEnter}）</li>
     * </ol>
     */
    @Override
    @Transactional
    public boolean updateBorrow(String borrowId, BorrowForm formData) {
        BorrowPO borrowPO = this.getById(borrowId);
        if (borrowPO == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (borrowPO.getRealityReturnTime() != null) {
            throw new BusinessException("已还书的不可操作");
        }
        formData.setUserId(borrowPO.getUserId());
        BorrowPO borrow = borrowConverter.toPo(formData);
        borrow.setId(borrowId);
        boolean returned = borrow.getRealityReturnTime() != null;
        if (returned) {
            StockForm stockForm = new StockForm();
            stockForm.setIsbn(borrowPO.getIsbn());
            stockForm.setStock(1);
            stockService.borrowEnter(stockForm);
        }
        boolean updated = this.updateById(borrow);
        if (updated && returned) {
            recommendationService.invalidateUserCache(borrowPO.getUserId());
            reservationService.promoteQueue(borrowPO.getIsbn());
        }
        return updated;
    }

    /** 委托给 BorrowNotificationService 执行发送（无视去重） */
    @Override
    public void sendReminder(String borrowId) {
        borrowNotificationService.sendReminder(borrowId);
    }

    /** 馆员代借场景下，借阅记录必须关联具体用户 */
    private void validateBorrowUserId(BorrowForm formData) {
        if (formData.getUserId() == null) {
            throw new BusinessException("代借用户不能为空");
        }
    }
}
