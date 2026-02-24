package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.PublishForm;
import org.dwtech.common.core.entity.query.PublishPageQuery;
import org.dwtech.common.core.entity.vo.PublishPageVO;
import org.dwtech.system.service.PublishService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/publish")
public class PublishController {
    private final PublishService publishService;

    @GetMapping("/page")
    PageResult<PublishPageVO> getPublishPage(@Valid PublishPageQuery queryParams) {
        IPage<PublishPageVO> result = publishService.getPublishPage(queryParams);
        return PageResult.success(result);
    }

    @GetMapping("/options")
    Result<List<Option<Long>>> getPublishOptions() {
        List<Option<Long>> list = publishService.getPublishOptions();
        return Result.success(list);
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('lib:publish:edit')")
    Result<PublishForm> getPublishForm(@PathVariable("id") Long id) {
        PublishForm publishForm = publishService.getPublishForm(id);
        return Result.success(publishForm);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('lib:publish:add')")
    Result<?> addPublish(@Valid @RequestBody PublishForm publishForm) {
        boolean result = publishService.savePublish(publishForm);
        return Result.judge(result);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPerm('lib:publish:edit')")
    Result<?> updatePublish(@Valid @RequestBody PublishForm publishForm) {
        boolean result = publishService.updatePublish(publishForm);
        return Result.judge(result);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('lib:publish:delete')")
    Result<?> deletePublish(@PathVariable("ids") List<Long> ids) {
        boolean result = publishService.deletePublish(ids);
        return Result.judge(result);
    }
}
