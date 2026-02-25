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
 * RoleConverter
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface RoleConverter {

    /**
     * 用途：转换为 page vo。
     * 
     * @param rolePage role page
     * @return 分页结果
     */
    Page<RolePageVO> toPageVo(Page<RolePO> rolePage);

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "name")
    })
    /**
     * 用途：转换为 option。
     * 
     * @param role role
     * @return 返回结果
     */
    Option<Long> toOption(RolePO role);

    /**
     * 用途：转换为 options。
     * 
     * @param roleList role list
     * @return 结果列表
     */
    List<Option<Long>> toOptions(List<RolePO> roleList);

    /**
     * 用途：转换为 po。
     * 
     * @param roleForm role form
     * @return 返回结果
     */
    RolePO toPo(RoleForm roleForm);

    /**
     * 用途：转换为 form。
     * 
     * @param entity entity
     * @return 返回结果
     */
    RoleForm toForm(RolePO entity);
}
