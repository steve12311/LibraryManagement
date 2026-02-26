package org.dwtech.system.service.impl;

import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.BorrowConverter;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    @Mock
    private BorrowConverter borrowConverter;

    @Mock
    private BookService bookService;

    @Mock
    private StockService stockService;

    private BorrowServiceImpl borrowService;

    @BeforeEach
    void setUp() {
        borrowService = spy(new BorrowServiceImpl(borrowConverter, bookService, stockService));
    }

    @Test
    void shouldThrowWhenBorrowRecordNotFound() {
        BorrowForm form = new BorrowForm();
        when(borrowConverter.toPo(form)).thenReturn(new BorrowPO());
        doReturn(null).when(borrowService).getById("borrow-1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.updateBorrow("borrow-1", form));

        assertThat(exception.getMessage()).isEqualTo("借阅记录不存在");
    }

    @Test
    void shouldRejectUpdateWhenBookAlreadyReturned() {
        BorrowForm form = new BorrowForm();
        when(borrowConverter.toPo(form)).thenReturn(new BorrowPO());

        BorrowPO existed = new BorrowPO();
        existed.setRealityReturnTime(LocalDateTime.now());
        doReturn(existed).when(borrowService).getById("borrow-1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.updateBorrow("borrow-1", form));

        assertThat(exception.getMessage()).isEqualTo("已还书的不可操作");
    }
}
