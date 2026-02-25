package org.dwtech.system.service;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.query.DeptQuery;
import org.dwtech.system.model.vo.DeptVO;

import java.util.List;
/**
 * DeptService
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface DeptService {
    List<DeptVO> getDeptList(DeptQuery queryParams);

    List<Option<Long>> listDeptOptions();
}
