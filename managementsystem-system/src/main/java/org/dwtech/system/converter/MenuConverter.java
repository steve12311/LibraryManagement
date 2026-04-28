package org.dwtech.system.converter;

import org.dwtech.system.model.form.MenuForm;
import org.dwtech.system.model.form.PermitForm;
import org.dwtech.system.model.entity.MenuPO;
import org.dwtech.system.model.vo.MenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
/**
 * 菜单对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface MenuConverter {
    /**
     * MenuPO → MenuVO
     */
    MenuVO toVo(MenuPO po);

    /**
     * MenuPO → MenuForm
     */
    MenuForm toForm(MenuPO entity);

    @Mappings({
            @Mapping(target = "label", source = "name"),
            @Mapping(target = "value", source = "perm")
    })
    /**
     * MenuPO → PermitForm
     */
    PermitForm toPermitForm(MenuPO entity);

    @Mappings({
            @Mapping(target = "name", source = "label"),
            @Mapping(target = "perm", source = "value"),
            @Mapping(target = "type", expression = "java(org.dwtech.common.enmus.MenuTypeEnum.BUTTON.getValue())")
    })
    /**
     * PermitForm → MenuPO
     */
    MenuPO toPo(PermitForm permitForm);

    /**
     * List<PermitForm> → List<MenuPO>
     */
    List<MenuPO> toPos(List<PermitForm> permitForms);

    /**
     * List<MenuPO> → List<PermitForm>
     */
    List<PermitForm> toPermitForms(List<MenuPO> entities);

    /**
     * MenuForm → MenuPO
     */
    MenuPO toPo(MenuForm menuForm);
}
