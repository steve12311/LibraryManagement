package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.PublishForm;
import org.dwtech.system.model.query.PublishPageQuery;
import org.dwtech.system.model.vo.PublishPageVO;
import org.dwtech.system.service.PublishService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * PublishController
 *
 * @author steve12311
 * @since 2026-02-22
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/publish")
public class PublishController {
    private final PublishService publishService;

    @GetMapping("/page")
    public PageResult<PublishPageVO> getPublishPage(@Valid PublishPageQuery queryParams) {
        IPage<PublishPageVO> result = publishService.getPublishPage(queryParams);
        return PageResult.success(result);
    }

    @GetMapping("/options")
    public Result<List<Option<Long>>> listPublishOptions() {
        List<Option<Long>> list = publishService.listPublishOptions();
        return Result.success(list);
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('lib:publish:edit')")
    public Result<PublishForm> getPublishForm(@PathVariable("id") Long id) {
        PublishForm publishForm = publishService.getPublishForm(id);
        return Result.success(publishForm);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('lib:publish:add')")
    @RepeatSubmit
    public Result<?> addPublish(@Valid @RequestBody PublishForm publishForm) {
        boolean result = publishService.savePublish(publishForm);
        return Result.judge(result);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPerm('lib:publish:edit')")
    @RepeatSubmit
    public Result<?> updatePublish(@Valid @RequestBody PublishForm publishForm) {
        boolean result = publishService.updatePublish(publishForm);
        return Result.judge(result);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('lib:publish:delete')")
    @RepeatSubmit
    public Result<?> deletePublish(@PathVariable("ids") List<Long> ids) {
        boolean result = publishService.deletePublish(ids);
        return Result.judge(result);
    }
}
