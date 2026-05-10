package org.dwtech.system.converter;

import org.dwtech.system.model.entity.BookshelfPO;
import org.dwtech.system.model.entity.LibraryFloorPO;
import org.dwtech.system.model.form.BookshelfForm;
import org.dwtech.system.model.form.LibraryFloorForm;
import org.dwtech.system.model.vo.BookshelfVO;
import org.dwtech.system.model.vo.LibraryFloorVO;
import org.dwtech.system.model.vo.PublicLibraryFloorVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 书架地图对象转换器
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Mapper(componentModel = "spring")
public interface LibraryMapConverter {

    LibraryFloorPO toFloorPo(LibraryFloorForm form);

    LibraryFloorVO toFloorVo(LibraryFloorPO po);

    List<LibraryFloorVO> toFloorVoList(List<LibraryFloorPO> list);

    PublicLibraryFloorVO toPublicFloorVo(LibraryFloorPO po);

    List<PublicLibraryFloorVO> toPublicFloorVoList(List<LibraryFloorPO> list);

    BookshelfPO toBookshelfPo(BookshelfForm form);

    BookshelfVO toBookshelfVo(BookshelfPO po);

    List<BookshelfVO> toBookshelfVoList(List<BookshelfPO> list);
}
