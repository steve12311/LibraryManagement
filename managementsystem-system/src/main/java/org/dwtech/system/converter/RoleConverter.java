package org.dwtech.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.RoleForm;
import org.dwtech.system.model.entity.RolePO;
import org.dwtech.system.model.vo.RolePageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * 角色对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface RoleConverter {

    /**
     * Page<RolePO> → Page<RolePageVO>
     */
    Page<RolePageVO> toPageVo(Page<RolePO> rolePage);

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "name")
    })
    /**
     * RolePO → Option
     */
    Option<Long> toOption(RolePO role);

    /**
     * List<RolePO> → List<Option>
     */
    List<Option<Long>> toOptions(List<RolePO> roleList);

    /**
     * RoleForm → RolePO
     */
    RolePO toPo(RoleForm roleForm);

    /**
     * RolePO → RoleForm
     */
    RoleForm toForm(RolePO entity);
}
