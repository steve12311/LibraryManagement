package org.dwtech.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.SystemConstants;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.po.RolePO;
import org.dwtech.common.core.entity.query.RolePageQuery;
import org.dwtech.common.core.entity.vo.RolePageVO;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.converter.RoleConverter;
import org.dwtech.system.mapper.RoleMapper;
import org.dwtech.system.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RolePO> implements RoleService {
    private final RoleConverter roleConverter;

    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        log.info("{}", roles);
        return this.baseMapper.getMaximumDataScope(roles);
    }

    @Override
    public Page<RolePageVO> getRolePage(RolePageQuery queryParams) {
        // 查询参数
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        String keywords = queryParams.getKeywords();

        // 查询数据
        Page<RolePO> rolePage = this.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<RolePO>()
                        .and(StrUtil.isNotBlank(keywords),
                                wrapper ->
                                        wrapper.like(RolePO::getName, keywords)
                                                .or()
                                                .like(RolePO::getCode, keywords)
                        )
                        .ne(!SecurityUtils.isRoot(), RolePO::getCode, SystemConstants.ROOT_ROLE_CODE) // 非超级管理员不显示超级管理员角色
                        .orderByAsc(RolePO::getSort).orderByDesc(RolePO::getCreateTime).orderByDesc(RolePO::getUpdateTime)
        );

        // 实体转换
        return roleConverter.toPageVo(rolePage);
    }

    @Override
    public List<Option<Long>> listRoleOptions() {
        // 查询数据
        List<RolePO> roleList = this.list(new LambdaQueryWrapper<RolePO>()
                .ne(!SecurityUtils.isRoot(), RolePO::getCode, SystemConstants.ROOT_ROLE_CODE)
                .select(RolePO::getId, RolePO::getName)
                .orderByAsc(RolePO::getSort)
        );

        // 实体转换
        return roleConverter.toOptions(roleList);
    }
}
