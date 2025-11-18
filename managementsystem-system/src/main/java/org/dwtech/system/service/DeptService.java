package org.dwtech.system.service;

import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.query.DeptQuery;
import org.dwtech.common.core.entity.vo.DeptVO;

import java.util.List;

public interface DeptService {
    List<DeptVO> getDeptList(DeptQuery queryParams);

    List<Option<Long>> listDeptOptions();
}
