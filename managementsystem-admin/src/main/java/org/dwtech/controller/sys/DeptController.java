package org.dwtech.controller.sys;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.query.DeptQuery;
import org.dwtech.system.model.vo.DeptVO;
import org.dwtech.system.service.DeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * DeptController
 *
 * @author steve12311
 * @since 2025-11-18
 */

@RestController
@RequestMapping("/api/v1/dept")
@RequiredArgsConstructor
public class DeptController {
    private final DeptService deptService;

    /**
     * 用途：获取 dept list 信息。
     * 
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:dept:list')")
    public Result<List<DeptVO>> getDeptList(
            DeptQuery queryParams
    ) {
        List<DeptVO> list = deptService.getDeptList(queryParams);
        return Result.success(list);
    }

    /**
     * 用途：获取 dept options 信息。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('sys:dept:list')")
    public Result<List<Option<Long>>> getDeptOptions() {
        List<Option<Long>> list = deptService.listDeptOptions();
        return Result.success(list);
    }
}
