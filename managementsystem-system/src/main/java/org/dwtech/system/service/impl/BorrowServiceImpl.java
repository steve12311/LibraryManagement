package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.enmus.DataScopeEnum;
import org.dwtech.system.model.bo.BorrowBO;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.query.BorrowPageQuery;
import org.dwtech.system.model.vo.BorrowVO;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.common.utils.uuid.UUID;
import org.dwtech.system.converter.BorrowConverter;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * BorrowServiceImpl
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

    /**
     * 用途：获取 borrow page 信息。
     * 
     * @param queryParams query params
     * @return 分页结果
     */
    @Override
    public IPage<BorrowVO> getBorrowPage(BorrowPageQuery queryParams) {
        // 参数构建
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<BorrowBO> page = new Page<>(pageNum, pageSize);

        Page<BorrowBO> borrowPage = this.baseMapper.getBorrowPage(page, queryParams);

        return borrowConverter.toPageVo(borrowPage);
    }

    /**
     * 用途：保存 borrow。
     * 
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @Transactional
    public boolean saveBorrow(BorrowForm formData) {
        bindCurrentUserForSelfScope(formData);
        String uuid = UUID.randomUUID().toString();
        String bookName = bookService.getBookByIsbn(formData.getIsbn()).getName();

        BorrowPO borrow = borrowConverter.toPo(formData);
        borrow.setBookName(bookName);
        borrow.setId(uuid);
        this.saveOrUpdate(borrow);

        StockForm stockForm = new StockForm();
        stockForm.setIsbn(formData.getIsbn());
        stockForm.setStock(1);
        stockService.borrowOut(stockForm);
        return true;
    }

    /**
     * 用途：更新 borrow。
     * 
     * @param borrowId borrow ID
     * @param formData form data
     * @return 操作结果，true 表示成功，false 表示失败
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
        validateBorrowWriteScope(borrowPO);
        formData.setUserId(borrowPO.getUserId());
        BorrowPO borrow = borrowConverter.toPo(formData);
        borrow.setId(borrowId);
        if (borrow.getRealityReturnTime() != null) {
            StockForm stockForm = new StockForm();
            stockForm.setIsbn(borrowPO.getIsbn());
            stockForm.setStock(1);
            stockService.borrowEnter(stockForm);
        }
        return this.updateById(borrow);
    }

    /**
     * SELF 数据权限用户只能为自己创建借阅记录。
     */
    private void bindCurrentUserForSelfScope(BorrowForm formData) {
        if (!isSelfDataScope()) {
            return;
        }
        Long currentUserId = requireCurrentUserId();
        Long borrowUserId = formData.getUserId();
        if (borrowUserId != null && !currentUserId.equals(borrowUserId)) {
            throw new BusinessException("无权为他人创建借阅记录");
        }
        formData.setUserId(currentUserId);
    }

    /**
     * SELF 数据权限用户只能操作自己的借阅记录。
     */
    private void validateBorrowWriteScope(BorrowPO borrowPO) {
        if (!isSelfDataScope()) {
            return;
        }
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(borrowPO.getUserId())) {
            throw new BusinessException("无权操作他人借阅记录");
        }
    }

    private boolean isSelfDataScope() {
        return DataScopeEnum.SELF.getValue().equals(SecurityUtils.getDataScope());
    }

    private Long requireCurrentUserId() {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null) {
            throw new BusinessException("未获取到当前登录用户");
        }
        return currentUserId;
    }
}
