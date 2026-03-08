package org.dwtech.system.service.impl;

import org.dwtech.common.core.entity.SysUserDetails;
import org.dwtech.common.enmus.DataScopeEnum;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.BorrowConverter;
import org.dwtech.system.mapper.BorrowMapper;
import org.dwtech.system.model.entity.BorrowPO;
import org.dwtech.system.model.form.BorrowForm;
import org.dwtech.system.service.BookService;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectSaveBorrowForOtherUserWhenDataScopeIsSelf() {
        setCurrentUser(1001L, DataScopeEnum.SELF.getValue());
        BorrowForm formData = new BorrowForm();
        formData.setUserId(2002L);
        formData.setIsbn("9787300000001");

        assertThatThrownBy(() -> borrowService.saveBorrow(formData))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权为他人创建借阅记录");

        verifyNoInteractions(bookService, borrowConverter, stockService, borrowMapper);
    }

    @Test
    void shouldRejectUpdateBorrowForOtherUserWhenDataScopeIsSelf() {
        setCurrentUser(1001L, DataScopeEnum.SELF.getValue());
        BorrowPO borrowPO = new BorrowPO();
        borrowPO.setUserId(2002L);
        when(borrowMapper.selectById("borrow-1")).thenReturn(borrowPO);

        assertThatThrownBy(() -> borrowService.updateBorrow("borrow-1", new BorrowForm()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作他人借阅记录");

        verifyNoInteractions(borrowConverter, stockService);
        verify(borrowMapper, never()).updateById(any(BorrowPO.class));
    }

    @Test
    void shouldKeepOriginalBorrowUserWhenUpdateBorrow() {
        setCurrentUser(1001L, DataScopeEnum.ALL.getValue());
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

    private void setCurrentUser(Long userId, Integer dataScope) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(userId);
        userDetails.setDataScope(dataScope);
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }
}
