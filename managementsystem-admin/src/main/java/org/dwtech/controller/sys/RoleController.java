package org.dwtech.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.query.RolePageQuery;
import org.dwtech.common.core.entity.vo.RolePageVO;
import org.dwtech.system.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/page")
    public PageResult<RolePageVO> getRolePage(
            RolePageQuery queryParams
    ) {
        Page<RolePageVO> result = roleService.getRolePage(queryParams);
        return PageResult.success(result);
    }

    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoleOptions() {
        List<Option<Long>> list = roleService.listRoleOptions();
        return Result.success(list);
    }
}
