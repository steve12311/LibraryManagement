package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.RoleForm;
import org.dwtech.common.core.entity.po.RolePO;
import org.dwtech.common.core.entity.vo.RolePageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleConverter {

    Page<RolePageVO> toPageVo(Page<RolePO> rolePage);

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "name")
    })
    Option<Long> toOption(RolePO role);

    List<Option<Long>> toOptions(List<RolePO> roleList);

    RolePO toPo(RoleForm roleForm);

    RoleForm toForm(RolePO entity);
}
