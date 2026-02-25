package org.dwtech.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.entity.DeptPO;
import org.dwtech.system.model.query.DeptQuery;
import org.dwtech.system.model.vo.DeptVO;
import org.dwtech.common.enmus.StatusEnum;
import org.dwtech.system.converter.DeptConverter;
import org.dwtech.system.mapper.DeptMapper;
import org.dwtech.system.service.DeptService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * DeptServiceImpl
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, DeptPO> implements DeptService {
    private final DeptConverter deptConverter;

    /**
     * 用途：获取 dept list 信息。
     * 
     * @param queryParams query params
     * @return 结果列表
     */
    @Override
    public List<DeptVO> getDeptList(DeptQuery queryParams) {
        // 查询参数
        String keywords = queryParams.getKeywords();
        Integer status = queryParams.getStatus();

        // 查询数据
        List<DeptPO> deptList = this.list(
                new LambdaQueryWrapper<DeptPO>()
                        .like(StrUtil.isNotBlank(keywords), DeptPO::getName, keywords)
                        .eq(status != null, DeptPO::getStatus, status)
                        .orderByAsc(DeptPO::getSort)
        );

        if (CollectionUtil.isEmpty(deptList)) {
            return Collections.emptyList();
        }

        // 获取所有部门ID
        Set<Long> deptIds = deptList.stream()
                .map(DeptPO::getId)
                .collect(Collectors.toSet());
        // 获取父节点ID
        Set<Long> parentIds = deptList.stream()
                .map(DeptPO::getParentId)
                .collect(Collectors.toSet());
        // 获取根节点ID（递归的起点），即父节点ID中不包含在部门ID中的节点，注意这里不能拿顶级部门 O 作为根节点，因为部门筛选的时候 O 会被过滤掉
        List<Long> rootIds = CollectionUtil.subtractToList(parentIds, deptIds);

        // 递归生成部门树形列表
        return rootIds.stream()
                .flatMap(rootId -> recurDeptList(rootId, deptList).stream())
                .toList();
    }

    /**
     * 用途：查询 dept options 列表。
     * 
     * 入参：无。
     * @return 结果列表
     */
    @Override
    public List<Option<Long>> listDeptOptions() {
        List<DeptPO> deptList = this.list(new LambdaQueryWrapper<DeptPO>()
                .eq(DeptPO::getStatus, StatusEnum.ENABLE.getValue())
                .select(DeptPO::getId, DeptPO::getParentId, DeptPO::getName)
                .orderByAsc(DeptPO::getSort)
        );
        if (CollectionUtil.isEmpty(deptList)) {
            return Collections.emptyList();
        }

        Set<Long> deptIds = deptList.stream()
                .map(DeptPO::getId)
                .collect(Collectors.toSet());

        Set<Long> parentIds = deptList.stream()
                .map(DeptPO::getParentId)
                .collect(Collectors.toSet());

        List<Long> rootIds = CollectionUtil.subtractToList(parentIds, deptIds);

        // 递归生成部门树形列表
        return rootIds.stream()
                .flatMap(rootId -> recurDeptTreeOptions(rootId, deptList).stream())
                .toList();
    }

    /**
     * 用途：执行 recur dept tree options 操作。
     * 
     * 递归生成部门表格层级列表
     *
     * @param parentId 父ID
     * @param deptList 部门列表
     * @return 部门表格层级列表
     */
    public static List<Option<Long>> recurDeptTreeOptions(long parentId, List<DeptPO> deptList) {
        return CollectionUtil.emptyIfNull(deptList).stream()
                .filter(dept -> dept.getParentId().equals(parentId))
                .map(dept -> {
                    Option<Long> option = new Option<>(dept.getId(), dept.getName());
                    List<Option<Long>> children = recurDeptTreeOptions(dept.getId(), deptList);
                    if (CollectionUtil.isNotEmpty(children)) {
                        option.setChildren(children);
                    }
                    return option;
                })
                .collect(Collectors.toList());
    }

    /**
     * 用途：执行 recur dept list 操作。
     * 
     * 递归生成部门树形列表
     *
     * @param parentId 父ID
     * @param deptList 部门列表
     * @return 部门树形列表
     */
    public List<DeptVO> recurDeptList(Long parentId, List<DeptPO> deptList) {
        return deptList.stream()
                .filter(dept -> dept.getParentId().equals(parentId))
                .map(dept -> {
                    DeptVO deptVO = deptConverter.toVo(dept);
                    List<DeptVO> children = recurDeptList(dept.getId(), deptList);
                    deptVO.setChildren(children);
                    return deptVO;
                }).toList();
    }
}
