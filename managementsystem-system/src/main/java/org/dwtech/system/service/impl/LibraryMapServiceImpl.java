package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.system.converter.LibraryMapConverter;
import org.dwtech.system.mapper.BookshelfMapper;
import org.dwtech.system.mapper.LibraryFloorMapper;
import org.dwtech.system.model.bo.BookshelfUsageBO;
import org.dwtech.system.model.entity.BookshelfPO;
import org.dwtech.system.model.entity.LibraryFloorPO;
import org.dwtech.system.model.form.BookshelfForm;
import org.dwtech.system.model.form.FloorOutlineForm;
import org.dwtech.system.model.form.LibraryFloorForm;
import org.dwtech.system.model.vo.BookshelfOptionVO;
import org.dwtech.system.model.vo.BookshelfVO;
import org.dwtech.system.model.vo.LibraryFloorVO;
import org.dwtech.system.model.vo.PublicBookshelfVO;
import org.dwtech.system.model.vo.PublicLibraryFloorDetailVO;
import org.dwtech.system.model.vo.PublicLibraryFloorVO;
import org.dwtech.system.model.vo.PublicShelfBookVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 图书馆书架地图服务实现
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Service
@RequiredArgsConstructor
public class LibraryMapServiceImpl implements org.dwtech.system.service.LibraryMapService {
    private static final int ENABLED = 1;

    private final LibraryFloorMapper libraryFloorMapper;
    private final BookshelfMapper bookshelfMapper;
    private final LibraryMapConverter libraryMapConverter;

    @Override
    public List<LibraryFloorVO> listFloors() {
        List<LibraryFloorPO> floors = libraryFloorMapper.selectList(new LambdaQueryWrapper<LibraryFloorPO>()
                .orderByAsc(LibraryFloorPO::getSort)
                .orderByAsc(LibraryFloorPO::getFloorNo)
        );
        return libraryMapConverter.toFloorVoList(floors);
    }

    @Override
    public List<PublicLibraryFloorVO> listPublicFloors() {
        List<LibraryFloorPO> floors = libraryFloorMapper.selectList(new LambdaQueryWrapper<LibraryFloorPO>()
                .eq(LibraryFloorPO::getStatus, ENABLED)
                .orderByAsc(LibraryFloorPO::getSort)
                .orderByAsc(LibraryFloorPO::getFloorNo)
        );
        return libraryMapConverter.toPublicFloorVoList(floors);
    }

    @Override
    @Transactional
    public LibraryFloorVO saveFloor(LibraryFloorForm form) {
        ensureFloorNoUnique(form.getFloorNo(), null);
        LibraryFloorPO floor = libraryMapConverter.toFloorPo(form);
        if (floor.getSort() == null) {
            floor.setSort(floor.getFloorNo());
        }
        libraryFloorMapper.insert(floor);
        return libraryMapConverter.toFloorVo(floor);
    }

    @Override
    @Transactional
    public LibraryFloorVO updateFloor(Long id, LibraryFloorForm form) {
        LibraryFloorPO existing = getRequiredFloor(id);
        ensureFloorNoUnique(form.getFloorNo(), id);
        LibraryFloorPO floor = libraryMapConverter.toFloorPo(form);
        floor.setId(id);
        floor.setOutlineJson(form.getOutlineJson() == null ? existing.getOutlineJson() : form.getOutlineJson());
        if (floor.getSort() == null) {
            floor.setSort(floor.getFloorNo());
        }
        libraryFloorMapper.updateById(floor);
        return libraryMapConverter.toFloorVo(libraryFloorMapper.selectById(id));
    }

    @Override
    @Transactional
    public LibraryFloorVO updateFloorOutline(Long id, FloorOutlineForm form) {
        LibraryFloorPO floor = getRequiredFloor(id);
        floor.setOutlineJson(form.getOutlineJson());
        libraryFloorMapper.updateById(floor);
        return libraryMapConverter.toFloorVo(libraryFloorMapper.selectById(id));
    }

    @Override
    @Transactional
    public boolean deleteFloor(Long id) {
        getRequiredFloor(id);
        if (bookshelfMapper.countByFloorId(id) > 0) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "楼层下存在书架，不能删除");
        }
        return libraryFloorMapper.deleteById(id) > 0;
    }

    @Override
    public List<BookshelfVO> listShelves(Long floorId) {
        getRequiredFloor(floorId);
        List<BookshelfPO> shelves = bookshelfMapper.selectList(new LambdaQueryWrapper<BookshelfPO>()
                .eq(BookshelfPO::getFloorId, floorId)
                .orderByAsc(BookshelfPO::getShelfNo)
                .orderByAsc(BookshelfPO::getId)
        );
        return attachShelfUsage(libraryMapConverter.toBookshelfVoList(shelves));
    }

    @Override
    @Transactional
    public BookshelfVO saveShelf(BookshelfForm form) {
        getRequiredFloor(form.getFloorId());
        ensureShelfNoUnique(form.getShelfNo(), null);
        BookshelfPO shelf = libraryMapConverter.toBookshelfPo(form);
        bookshelfMapper.insert(shelf);
        return toBookshelfVoWithUsage(shelf);
    }

    @Override
    @Transactional
    public BookshelfVO updateShelf(Long id, BookshelfForm form) {
        getRequiredShelf(id);
        getRequiredFloor(form.getFloorId());
        ensureShelfNoUnique(form.getShelfNo(), id);
        int usedStock = safeUsedStock(bookshelfMapper.sumStockByShelfId(id));
        if (form.getCapacity() < usedStock) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "书架容量不能小于已占用册数");
        }
        BookshelfPO shelf = libraryMapConverter.toBookshelfPo(form);
        shelf.setId(id);
        bookshelfMapper.updateById(shelf);
        return toBookshelfVoWithUsage(bookshelfMapper.selectById(id));
    }

    @Override
    @Transactional
    public boolean deleteShelf(Long id) {
        getRequiredShelf(id);
        if (bookshelfMapper.countStockByShelfId(id) > 0) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "书架已绑定图书，不能删除");
        }
        return bookshelfMapper.deleteById(id) > 0;
    }

    @Override
    public List<BookshelfOptionVO> listShelfOptions(boolean enabledOnly) {
        return bookshelfMapper.listShelfOptions(enabledOnly);
    }

    @Override
    public PublicLibraryFloorDetailVO getPublicFloorDetail(Long floorId) {
        LibraryFloorPO floor = getRequiredFloor(floorId);
        if (!Objects.equals(floor.getStatus(), ENABLED)) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "楼层不存在");
        }
        List<BookshelfPO> shelves = bookshelfMapper.selectList(new LambdaQueryWrapper<BookshelfPO>()
                .eq(BookshelfPO::getFloorId, floorId)
                .eq(BookshelfPO::getStatus, ENABLED)
                .orderByAsc(BookshelfPO::getShelfNo)
                .orderByAsc(BookshelfPO::getId)
        );
        List<Long> shelfIds = shelves.stream().map(BookshelfPO::getId).toList();
        Map<Long, Integer> usageMap = buildUsageMap(shelfIds);
        Map<Long, List<PublicShelfBookVO>> booksMap = buildPublicBookMap(shelfIds);

        PublicLibraryFloorDetailVO detail = new PublicLibraryFloorDetailVO();
        detail.setId(floor.getId());
        detail.setFloorNo(floor.getFloorNo());
        detail.setName(floor.getName());
        detail.setOutlineJson(floor.getOutlineJson());
        detail.setShelves(shelves.stream()
                .map(shelf -> toPublicBookshelfVo(shelf, usageMap, booksMap))
                .toList());
        return detail;
    }

    private LibraryFloorPO getRequiredFloor(Long floorId) {
        LibraryFloorPO floor = libraryFloorMapper.selectById(floorId);
        if (floor == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "楼层不存在");
        }
        return floor;
    }

    private BookshelfPO getRequiredShelf(Long shelfId) {
        BookshelfPO shelf = bookshelfMapper.selectById(shelfId);
        if (shelf == null) {
            throw new BusinessException(ResultCode.USER_RESOURCE_NOT_FOUND, "书架不存在");
        }
        return shelf;
    }

    private void ensureFloorNoUnique(Integer floorNo, Long excludeId) {
        Long count = libraryFloorMapper.selectCount(new LambdaQueryWrapper<LibraryFloorPO>()
                .eq(LibraryFloorPO::getFloorNo, floorNo)
                .ne(excludeId != null, LibraryFloorPO::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "楼层编号已存在");
        }
    }

    private void ensureShelfNoUnique(String shelfNo, Long excludeId) {
        Long count = bookshelfMapper.selectCount(new LambdaQueryWrapper<BookshelfPO>()
                .eq(BookshelfPO::getShelfNo, shelfNo)
                .ne(excludeId != null, BookshelfPO::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.USER_OPERATION_EXCEPTION, "书架号已存在");
        }
    }

    private BookshelfVO toBookshelfVoWithUsage(BookshelfPO shelf) {
        BookshelfVO vo = libraryMapConverter.toBookshelfVo(shelf);
        int usedStock = safeUsedStock(bookshelfMapper.sumStockByShelfId(shelf.getId()));
        fillShelfUsage(vo, usedStock);
        return vo;
    }

    private List<BookshelfVO> attachShelfUsage(List<BookshelfVO> shelves) {
        if (shelves == null || shelves.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> usageMap = buildUsageMap(shelves.stream().map(BookshelfVO::getId).toList());
        shelves.forEach(shelf -> fillShelfUsage(shelf, usageMap.getOrDefault(shelf.getId(), 0)));
        return shelves;
    }

    private Map<Long, Integer> buildUsageMap(List<Long> shelfIds) {
        if (shelfIds == null || shelfIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return bookshelfMapper.sumStockByShelfIds(shelfIds).stream()
                .collect(Collectors.toMap(BookshelfUsageBO::getShelfId, item -> safeUsedStock(item.getUsedStock())));
    }

    private Map<Long, List<PublicShelfBookVO>> buildPublicBookMap(List<Long> shelfIds) {
        if (shelfIds == null || shelfIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return bookshelfMapper.listPublicBooksByShelfIds(shelfIds).stream()
                .peek(book -> book.setCoverUrl(normalizePublicCoverUrl(book.getCoverUrl())))
                .collect(Collectors.groupingBy(PublicShelfBookVO::getShelfId));
    }

    private PublicBookshelfVO toPublicBookshelfVo(BookshelfPO shelf,
                                                  Map<Long, Integer> usageMap,
                                                  Map<Long, List<PublicShelfBookVO>> booksMap) {
        PublicBookshelfVO vo = new PublicBookshelfVO();
        vo.setShelfId(shelf.getId());
        vo.setShelfNo(shelf.getShelfNo());
        vo.setName(shelf.getName());
        vo.setX(shelf.getX());
        vo.setY(shelf.getY());
        vo.setWidth(shelf.getWidth());
        vo.setHeight(shelf.getHeight());
        vo.setAngle(shelf.getAngle());
        vo.setCapacity(shelf.getCapacity());
        vo.setUsedStock(usageMap.getOrDefault(shelf.getId(), 0));
        vo.setBooks(booksMap.getOrDefault(shelf.getId(), Collections.emptyList()));
        return vo;
    }

    private void fillShelfUsage(BookshelfVO shelf, int usedStock) {
        shelf.setUsedStock(usedStock);
        Integer capacity = shelf.getCapacity();
        shelf.setRemainingCapacity(capacity == null ? 0 : Math.max(capacity - usedStock, 0));
    }

    private static int safeUsedStock(Integer usedStock) {
        return usedStock == null ? 0 : usedStock;
    }

    private String normalizePublicCoverUrl(String cover) {
        if (cover == null || cover.isBlank()) {
            return cover;
        }
        if (cover.startsWith("/api/v1/files/")) {
            return cover;
        }
        if (cover.startsWith("/")) {
            return "/api/v1/files" + cover;
        }
        return cover;
    }
}
