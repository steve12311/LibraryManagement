package org.dwtech.system.service.impl;

import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.LibraryMapConverter;
import org.dwtech.system.converter.LibraryMapConverterImpl;
import org.dwtech.system.mapper.BookshelfMapper;
import org.dwtech.system.mapper.LibraryFloorMapper;
import org.dwtech.system.model.bo.BookshelfUsageBO;
import org.dwtech.system.model.entity.BookshelfPO;
import org.dwtech.system.model.entity.LibraryFloorPO;
import org.dwtech.system.model.form.BookshelfForm;
import org.dwtech.system.model.vo.PublicLibraryFloorDetailVO;
import org.dwtech.system.model.vo.PublicShelfBookVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryMapServiceImplTest {

    @Mock
    private LibraryFloorMapper libraryFloorMapper;

    @Mock
    private BookshelfMapper bookshelfMapper;

    private LibraryMapServiceImpl libraryMapService;

    @BeforeEach
    void setUp() {
        LibraryMapConverter converter = new LibraryMapConverterImpl();
        libraryMapService = new LibraryMapServiceImpl(libraryFloorMapper, bookshelfMapper, converter);
    }

    @Test
    void shouldRejectDeleteFloorWhenShelfExists() {
        LibraryFloorPO floor = new LibraryFloorPO();
        floor.setId(1L);
        when(libraryFloorMapper.selectById(1L)).thenReturn(floor);
        when(bookshelfMapper.countByFloorId(1L)).thenReturn(1);

        assertThatThrownBy(() -> libraryMapService.deleteFloor(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.USER_OPERATION_EXCEPTION);

        verify(libraryFloorMapper, never()).deleteById(1L);
    }

    @Test
    void shouldRejectDeleteShelfWhenStockBound() {
        BookshelfPO shelf = new BookshelfPO();
        shelf.setId(10L);
        when(bookshelfMapper.selectById(10L)).thenReturn(shelf);
        when(bookshelfMapper.countStockByShelfId(10L)).thenReturn(1);

        assertThatThrownBy(() -> libraryMapService.deleteShelf(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("书架已绑定图书，不能删除");

        verify(bookshelfMapper, never()).deleteById(10L);
    }

    @Test
    void shouldRejectShelfCapacityLowerThanUsedStock() {
        LibraryFloorPO floor = new LibraryFloorPO();
        floor.setId(1L);
        BookshelfPO shelf = new BookshelfPO();
        shelf.setId(10L);
        shelf.setFloorId(1L);

        BookshelfForm form = buildShelfForm();
        form.setCapacity(4);

        when(bookshelfMapper.selectById(10L)).thenReturn(shelf);
        when(libraryFloorMapper.selectById(1L)).thenReturn(floor);
        when(bookshelfMapper.selectCount(any())).thenReturn(0L);
        when(bookshelfMapper.sumStockByShelfId(10L)).thenReturn(5);

        assertThatThrownBy(() -> libraryMapService.updateShelf(10L, form))
                .isInstanceOf(BusinessException.class)
                .hasMessage("书架容量不能小于已占用册数");

        verify(bookshelfMapper, never()).updateById(any(BookshelfPO.class));
    }

    @Test
    void shouldReturnPublicFloorDetailWithShelfBooks() {
        LibraryFloorPO floor = new LibraryFloorPO();
        floor.setId(1L);
        floor.setFloorNo(2);
        floor.setName("二层");
        floor.setOutlineJson("[{\"x\":0,\"y\":0}]");
        floor.setStatus(1);

        BookshelfPO shelf = buildShelf();
        BookshelfUsageBO usage = new BookshelfUsageBO();
        usage.setShelfId(10L);
        usage.setUsedStock(3);
        PublicShelfBookVO book = new PublicShelfBookVO();
        book.setShelfId(10L);
        book.setIsbn("9787300000001");
        book.setCoverUrl("/301");
        book.setName("Spring Boot 实战");

        when(libraryFloorMapper.selectById(1L)).thenReturn(floor);
        when(bookshelfMapper.selectList(any())).thenReturn(List.of(shelf));
        when(bookshelfMapper.sumStockByShelfIds(List.of(10L))).thenReturn(List.of(usage));
        when(bookshelfMapper.listPublicBooksByShelfIds(List.of(10L))).thenReturn(List.of(book));

        PublicLibraryFloorDetailVO detail = libraryMapService.getPublicFloorDetail(1L);

        assertThat(detail.getName()).isEqualTo("二层");
        assertThat(detail.getShelves()).hasSize(1);
        assertThat(detail.getShelves().getFirst().getUsedStock()).isEqualTo(3);
        assertThat(detail.getShelves().getFirst().getBooks()).hasSize(1);
        assertThat(detail.getShelves().getFirst().getBooks().getFirst().getCoverUrl()).isEqualTo("/api/v1/files/301");
    }

    private BookshelfForm buildShelfForm() {
        BookshelfForm form = new BookshelfForm();
        form.setFloorId(1L);
        form.setShelfNo("A-01");
        form.setName("A区一号架");
        form.setX(BigDecimal.ZERO);
        form.setY(BigDecimal.ZERO);
        form.setWidth(BigDecimal.valueOf(80));
        form.setHeight(BigDecimal.valueOf(32));
        form.setAngle(BigDecimal.ZERO);
        form.setCapacity(10);
        form.setStatus(1);
        return form;
    }

    private BookshelfPO buildShelf() {
        BookshelfPO shelf = new BookshelfPO();
        shelf.setId(10L);
        shelf.setFloorId(1L);
        shelf.setShelfNo("A-01");
        shelf.setName("A区一号架");
        shelf.setX(BigDecimal.ZERO);
        shelf.setY(BigDecimal.ZERO);
        shelf.setWidth(BigDecimal.valueOf(80));
        shelf.setHeight(BigDecimal.valueOf(32));
        shelf.setAngle(BigDecimal.ZERO);
        shelf.setCapacity(10);
        shelf.setStatus(1);
        return shelf;
    }
}
