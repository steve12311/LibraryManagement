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
    /**
     * 用途：获取 dept list 信息。
     * 
     * @param queryParams query params
     * @return 结果列表
     */
    List<DeptVO> getDeptList(DeptQuery queryParams);

    /**
     * 用途：查询 dept options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    List<Option<Long>> listDeptOptions();
}
