package org.dwtech.system.service;

import jakarta.validation.Valid;
import org.dwtech.system.model.form.BookshelfForm;
import org.dwtech.system.model.form.FloorOutlineForm;
import org.dwtech.system.model.form.LibraryFloorForm;
import org.dwtech.system.model.vo.BookshelfOptionVO;
import org.dwtech.system.model.vo.BookshelfVO;
import org.dwtech.system.model.vo.LibraryFloorVO;
import org.dwtech.system.model.vo.PublicLibraryFloorDetailVO;
import org.dwtech.system.model.vo.PublicLibraryFloorVO;

import java.util.List;

/**
 * 图书馆书架地图服务
 *
 * @author steve12311
 * @since 2026-05-10
 */
public interface LibraryMapService {

    List<LibraryFloorVO> listFloors();

    List<PublicLibraryFloorVO> listPublicFloors();

    LibraryFloorVO saveFloor(@Valid LibraryFloorForm form);

    LibraryFloorVO updateFloor(Long id, @Valid LibraryFloorForm form);

    LibraryFloorVO updateFloorOutline(Long id, @Valid FloorOutlineForm form);

    boolean deleteFloor(Long id);

    List<BookshelfVO> listShelves(Long floorId);

    BookshelfVO saveShelf(@Valid BookshelfForm form);

    BookshelfVO updateShelf(Long id, @Valid BookshelfForm form);

    boolean deleteShelf(Long id);

    List<BookshelfOptionVO> listShelfOptions(boolean enabledOnly);

    PublicLibraryFloorDetailVO getPublicFloorDetail(Long floorId);
}
