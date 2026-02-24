package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.bo.BorrowBO;
import org.dwtech.common.core.entity.form.BorrowForm;
import org.dwtech.common.core.entity.form.StockForm;
import org.dwtech.common.core.entity.po.BorrowPO;
import org.dwtech.common.core.entity.query.BorrowPageQuery;
import org.dwtech.common.core.entity.vo.BorrowVO;
import org.dwtech.common.utils.uuid.UUID;
import org.dwtech.system.converter.BorrowConverter;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.BorrowService;
import org.dwtech.system.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, BorrowPO> implements BorrowService {
    private final BorrowConverter borrowConverter;
    private final BookService bookService;
    private final StockService stockService;

    @Override
    public IPage<BorrowVO> getBorrowPage(BorrowPageQuery queryParams) {
        // 参数构建
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<BorrowBO> page = new Page<>(pageNum, pageSize);

        Page<BorrowBO> borrowPage = this.baseMapper.getBorrowPage(page, queryParams);

        return borrowConverter.toPageVo(borrowPage);
    }

    @Override
    @Transactional
    public boolean saveBorrow(BorrowForm formData) {
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

    @Override
    @Transactional
    public boolean updateBorrow(String borrowId, BorrowForm formData) {
        BorrowPO borrow = borrowConverter.toPo(formData);
        BorrowPO borrowPO = this.getById(borrowId);
        if (borrowPO.getRealityReturnTime() != null) {
            throw new RuntimeException("已还书的不可操作");
        }
        borrow.setId(borrowId);
        if (borrow.getRealityReturnTime() != null) {
            StockForm stockForm = new StockForm();
            stockForm.setIsbn(borrowPO.getIsbn());
            stockForm.setStock(1);
            stockService.borrowEnter(stockForm);
        }
        return this.updateById(borrow);
    }
}
