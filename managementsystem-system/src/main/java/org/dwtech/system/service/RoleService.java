package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.query.RolePageQuery;
import org.dwtech.common.core.entity.vo.RolePageVO;

import java.util.List;
import java.util.Set;

public interface RoleService {
    Integer getMaximumDataScope(Set<String> roles);

    Page<RolePageVO> getRolePage(RolePageQuery queryParams);

    List<Option<Long>> listRoleOptions();
}
