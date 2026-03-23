package org.dwtech.system.service.impl;

import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.BorrowConverter;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.model.bo.MyBorrowBO;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.model.form.StockForm;
import org.dwtech.system.model.query.MyBorrowPageQuery;
import org.dwtech.system.model.vo.MyBorrowPageVO;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    @Mock
    private BorrowConverter borrowConverter;

    @Mock
    private BookService bookService;

    @Mock
    private StockService stockService;

    @Mock
    private BorrowMapper borrowMapper;

    private BorrowServiceImpl borrowService;

    @BeforeEach
    void setUp() {
        borrowService = new BorrowServiceImpl(borrowConverter, bookService, stockService);
        ReflectionTestUtils.setField(borrowService, "baseMapper", borrowMapper);
    }

    @Test
    void shouldQueryCurrentUserBorrowPageWithCurrentUserId() {
        MyBorrowPageQuery queryParams = new MyBorrowPageQuery();
        queryParams.setPageNum(2);
        queryParams.setPageSize(5);
        queryParams.setStatus(1);

        Page<MyBorrowBO> mapperPage = new Page<>(2, 5);
        Page<MyBorrowPageVO> convertedPage = new Page<>(2, 5);
        when(borrowMapper.getCurrentUserBorrowPage(any(Page.class), eq(2002L), eq(queryParams))).thenReturn(mapperPage);
        when(borrowConverter.toMyBorrowPageVo(mapperPage)).thenReturn(convertedPage);

        assertThat(borrowService.getCurrentUserBorrowPage(2002L, queryParams)).isSameAs(convertedPage);

        ArgumentCaptor<Page<MyBorrowBO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(borrowMapper).getCurrentUserBorrowPage(pageCaptor.capture(), eq(2002L), eq(queryParams));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5);
        verify(borrowConverter).toMyBorrowPageVo(mapperPage);
    }

    @Test
    void shouldRejectSaveBorrowWhenBorrowUserMissing() {
        BorrowForm formData = new BorrowForm();
        formData.setIsbn("9787300000001");

        assertThatThrownBy(() -> borrowService.saveBorrow(formData))
                .isInstanceOf(BusinessException.class)
                .hasMessage("代借用户不能为空");

        verifyNoInteractions(bookService, borrowConverter, stockService, borrowMapper);
    }

    @Test
    void shouldRejectSaveBorrowWhenBookMissing() {
        BorrowForm formData = new BorrowForm();
        formData.setUserId(2002L);
        formData.setIsbn("9787300000001");

        when(bookService.getBookByIsbn("9787300000001")).thenReturn(null);

        assertThatThrownBy(() -> borrowService.saveBorrow(formData))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.USER_RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> borrowService.saveBorrow(formData))
                .isInstanceOf(BusinessException.class)
                .hasMessage("图书不存在");

        verifyNoInteractions(borrowConverter, stockService, borrowMapper);
    }

    @Test
    void shouldKeepAssignedBorrowUserWhenSaveBorrow() {
        BookForm bookForm = new BookForm();
        bookForm.setName("Spring Boot 实战");
        when(bookService.getBookByIsbn("9787300000001")).thenReturn(bookForm);

        BorrowPO convertedBorrow = new BorrowPO();
        when(borrowConverter.toPo(any(BorrowForm.class))).thenReturn(convertedBorrow);

        BorrowForm formData = new BorrowForm();
        formData.setUserId(2002L);
        formData.setIsbn("9787300000001");

        boolean result = borrowService.saveBorrow(formData);

        assertThat(result).isTrue();
        ArgumentCaptor<BorrowForm> borrowFormCaptor = ArgumentCaptor.forClass(BorrowForm.class);
        verify(borrowConverter).toPo(borrowFormCaptor.capture());
        assertThat(borrowFormCaptor.getValue().getUserId()).isEqualTo(2002L);
        assertThat(convertedBorrow.getBookName()).isEqualTo("Spring Boot 实战");
        assertThat(convertedBorrow.getId()).isNotBlank();

        ArgumentCaptor<StockForm> stockFormCaptor = ArgumentCaptor.forClass(StockForm.class);
        verify(stockService).borrowOut(stockFormCaptor.capture());
        assertThat(stockFormCaptor.getValue().getIsbn()).isEqualTo("9787300000001");
        assertThat(stockFormCaptor.getValue().getStock()).isEqualTo(1);
    }

    @Test
    void shouldKeepOriginalBorrowUserWhenUpdateBorrow() {
        BorrowPO existingBorrow = new BorrowPO();
        existingBorrow.setUserId(2002L);
        when(borrowMapper.selectById("borrow-2")).thenReturn(existingBorrow);

        BorrowPO convertedBorrow = new BorrowPO();
        when(borrowConverter.toPo(any(BorrowForm.class))).thenReturn(convertedBorrow);
        when(borrowMapper.updateById(convertedBorrow)).thenReturn(1);

        BorrowForm formData = new BorrowForm();
        formData.setUserId(3003L);

        boolean result = borrowService.updateBorrow("borrow-2", formData);

        assertThat(result).isTrue();
        ArgumentCaptor<BorrowForm> captor = ArgumentCaptor.forClass(BorrowForm.class);
        verify(borrowConverter).toPo(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(2002L);
        assertThat(convertedBorrow.getId()).isEqualTo("borrow-2");
    }
}
