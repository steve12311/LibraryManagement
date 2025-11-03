package org.dwtech.controller.lb;

import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.LibPublishDto;
import org.dwtech.service.LibPublishService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lib/publish")
public class LibPublishController extends BaseController {
    private final LibPublishService libPublishService;

    public LibPublishController(LibPublishService libPublishService) {
        this.libPublishService = libPublishService;
    }

    @GetMapping("/list")
    public AjaxResult getPublishList(LibPublishDto libPublishDto) {
        return AjaxResult.success(libPublishService.selectPublishList(libPublishDto));
    }

    @PostMapping
    public AjaxResult addPublish(@RequestBody LibPublishDto libPublishDto) {
        return AjaxResult.success(libPublishService.insertPublish(libPublishDto));
    }

    @PutMapping
    public AjaxResult editPublish(@RequestBody LibPublishDto libPublishDto) {
        return AjaxResult.success(libPublishService.updatePublish(libPublishDto));
    }

    @DeleteMapping("/{publishIds}")
    public AjaxResult deletePublish(@PathVariable("publishIds") Long[] publishIds) {
        return AjaxResult.success(libPublishService.deletePublish(publishIds));
    }
}
