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
 * 部门管理服务实现。通过递归方式构建部门树形列表和下拉选项树。
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, DeptPO> implements DeptService {
    private final DeptConverter deptConverter;

    /**
     * 查询部门树形列表。按关键词和状态筛选后，计算根节点并递归构建树形结构。
     *
     * @param queryParams 查询参数（关键词、状态）
     * @return 部门树形列表
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
     * 查询所有启用部门的下拉选项树。查询启用部门后，从根节点递归构建带层级的选项列表。
     *
     * @return 部门下拉选项树
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
     * 递归生成部门下拉选项树，从指定父节点开始筛选直接子节点并递归构建 children。
     *
     * @param parentId 父节点 ID
     * @param deptList 全量部门列表
     * @return 部门下拉选项树
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
     * 递归生成部门树形列表，从指定父节点开始筛选直接子部门并递归构建 children。
     *
     * @param parentId 父节点 ID
     * @param deptList 全量部门列表
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
