package org.dwtech.controller.lib;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
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

    /**
     * 分页查询出版社列表。
     * 支持按出版社名称、联系人等条件筛选。
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('lib:publish:view')")
    public PageResult<PublishPageVO> getPublishPage(@Valid PublishPageQuery queryParams) {
        IPage<PublishPageVO> result = publishService.getPublishPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 查询出版社选项列表。
     * 返回出版社的 ID 和名称键值对，用于前端下拉选择器。
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('lib:publish:view')")
    public Result<List<Option<Long>>> listPublishOptions() {
        List<Option<Long>> list = publishService.listPublishOptions();
        return Result.success(list);
    }

    /**
     * 根据 ID 获取出版社表单数据，用于编辑时回显。
     *
     * @param id 出版社 ID
     */
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('lib:publish:edit')")
    public Result<PublishForm> getPublishForm(@PathVariable("id") Long id) {
        PublishForm publishForm = publishService.getPublishForm(id);
        return Result.success(publishForm);
    }

    /**
     * 新增出版社。
     * 接收出版社表单数据，校验名称唯一性后写入数据库并记录操作日志。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('lib:publish:add')")
    @RepeatSubmit
    @OperLog(module = "出版社管理", action = "新增出版社", bizId = "#p0.name")
    public Result<?> addPublish(@Valid @RequestBody PublishForm publishForm) {
        boolean result = publishService.savePublish(publishForm);
        return Result.judge(result);
    }

    /**
     * 更新出版社信息。
     * 根据表单中的 ID 查找并更新出版社资料，校验名称唯一性并记录操作日志。
     */
    @PutMapping
    @PreAuthorize("@ss.hasPerm('lib:publish:edit')")
    @RepeatSubmit
    @OperLog(module = "出版社管理", action = "修改出版社", bizId = "#p0.id")
    public Result<?> updatePublish(@Valid @RequestBody PublishForm publishForm) {
        boolean result = publishService.updatePublish(publishForm);
        return Result.judge(result);
    }

    /**
     * 批量删除出版社。
     * 根据主键 ID 列表删除出版社记录，若有关联图书则阻止删除。
     *
     * @param ids 出版社 ID 列表，支持批量删除
     */
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('lib:publish:delete')")
    @RepeatSubmit
    @OperLog(module = "出版社管理", action = "删除出版社", bizId = "#p0")
    public Result<?> deletePublish(@PathVariable("ids") List<Long> ids) {
        boolean result = publishService.deletePublish(ids);
        return Result.judge(result);
    }
}
