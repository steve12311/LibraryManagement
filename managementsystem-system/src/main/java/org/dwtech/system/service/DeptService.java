package org.dwtech.system.service;

import org.dwtech.common.model.Option;
import org.dwtech.system.model.query.DeptQuery;
import org.dwtech.system.model.vo.DeptVO;

import java.util.List;
/**
 * 部门管理服务，提供部门树形列表查询与下拉选项功能。
 *
 * @author steve12311
 * @since 2025-11-18
 */

public interface DeptService {
    /**
     * 查询部门树形列表。可按关键词和状态筛选，返回按排序字段排列的树形结构。
     *
     * @param queryParams 查询参数（关键词、状态）
     * @return 部门树形列表，含层级 children
     */
    List<DeptVO> getDeptList(DeptQuery queryParams);

    /**
     * 查询所有启用部门的下拉选项树，供前端部门选择器使用。
     *
     * @return 部门下拉选项树
     */
    List<Option<Long>> listDeptOptions();
}
