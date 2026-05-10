package org.dwtech.controller.lib;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.form.BookshelfForm;
import org.dwtech.system.model.form.FloorOutlineForm;
import org.dwtech.system.model.form.LibraryFloorForm;
import org.dwtech.system.model.vo.BookshelfOptionVO;
import org.dwtech.system.model.vo.BookshelfVO;
import org.dwtech.system.model.vo.LibraryFloorVO;
import org.dwtech.system.service.LibraryMapService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 书架地图管理接口
 *
 * @author steve12311
 * @since 2026-05-10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/library-map")
public class LibraryMapController {
    private final LibraryMapService libraryMapService;

    @GetMapping("/floors")
    @PreAuthorize("@ss.hasPerm('lib:map:list')")
    public Result<List<LibraryFloorVO>> listFloors() {
        return Result.success(libraryMapService.listFloors());
    }

    @PostMapping("/floors")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:add')")
    @OperLog(module = "书架地图", action = "新增楼层", bizId = "#p0.floorNo")
    public Result<LibraryFloorVO> saveFloor(@Valid @RequestBody LibraryFloorForm form) {
        return Result.success(libraryMapService.saveFloor(form));
    }

    @PutMapping("/floors/{id}")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:edit')")
    @OperLog(module = "书架地图", action = "修改楼层", bizId = "#p0")
    public Result<LibraryFloorVO> updateFloor(@PathVariable("id") Long id,
                                              @Valid @RequestBody LibraryFloorForm form) {
        return Result.success(libraryMapService.updateFloor(id, form));
    }

    @PutMapping("/floors/{id}/outline")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:edit')")
    @OperLog(module = "书架地图", action = "修改楼层轮廓", bizId = "#p0")
    public Result<LibraryFloorVO> updateFloorOutline(@PathVariable("id") Long id,
                                                     @Valid @RequestBody FloorOutlineForm form) {
        return Result.success(libraryMapService.updateFloorOutline(id, form));
    }

    @DeleteMapping("/floors/{id}")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:delete')")
    @OperLog(module = "书架地图", action = "删除楼层", bizId = "#p0")
    public Result<?> deleteFloor(@PathVariable("id") Long id) {
        return Result.judge(libraryMapService.deleteFloor(id));
    }

    @GetMapping("/floors/{floorId}/shelves")
    @PreAuthorize("@ss.hasPerm('lib:map:view')")
    public Result<List<BookshelfVO>> listShelves(@PathVariable("floorId") Long floorId) {
        return Result.success(libraryMapService.listShelves(floorId));
    }

    @PostMapping("/shelves")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:add')")
    @OperLog(module = "书架地图", action = "新增书架", bizId = "#p0.shelfNo")
    public Result<BookshelfVO> saveShelf(@Valid @RequestBody BookshelfForm form) {
        return Result.success(libraryMapService.saveShelf(form));
    }

    @PutMapping("/shelves/{id}")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:edit')")
    @OperLog(module = "书架地图", action = "修改书架", bizId = "#p0")
    public Result<BookshelfVO> updateShelf(@PathVariable("id") Long id,
                                           @Valid @RequestBody BookshelfForm form) {
        return Result.success(libraryMapService.updateShelf(id, form));
    }

    @DeleteMapping("/shelves/{id}")
    @RepeatSubmit
    @PreAuthorize("@ss.hasPerm('lib:map:delete')")
    @OperLog(module = "书架地图", action = "删除书架", bizId = "#p0")
    public Result<?> deleteShelf(@PathVariable("id") Long id) {
        return Result.judge(libraryMapService.deleteShelf(id));
    }

    @GetMapping("/shelves/options")
    @PreAuthorize("@ss.hasPerm('sys:stock:list') or @ss.hasPerm('lib:map:view')")
    public Result<List<BookshelfOptionVO>> listShelfOptions(
            @RequestParam(value = "enabledOnly", defaultValue = "true") boolean enabledOnly) {
        return Result.success(libraryMapService.listShelfOptions(enabledOnly));
    }
}
