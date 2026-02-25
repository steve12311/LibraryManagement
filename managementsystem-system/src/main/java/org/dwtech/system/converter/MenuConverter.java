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
 * MenuConverter
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface MenuConverter {
    /**
     * 用途：转换为 vo。
     * 
     * @param po po
     * @return 返回结果
     */
    MenuVO toVo(MenuPO po);

    /**
     * 用途：转换为 form。
     * 
     * @param entity entity
     * @return 返回结果
     */
    MenuForm toForm(MenuPO entity);

    @Mappings({
            @Mapping(target = "label", source = "name"),
            @Mapping(target = "value", source = "perm")
    })
    /**
     * 用途：转换为 permit form。
     * 
     * @param entity entity
     * @return 返回结果
     */
    PermitForm toPermitForm(MenuPO entity);

    @Mappings({
            @Mapping(target = "name", source = "label"),
            @Mapping(target = "perm", source = "value"),
            @Mapping(target = "type", expression = "java(org.dwtech.common.enmus.MenuTypeEnum.BUTTON.getValue())")
    })
    /**
     * 用途：转换为 po。
     * 
     * @param permitForm permit form
     * @return 返回结果
     */
    MenuPO toPo(PermitForm permitForm);

    /**
     * 用途：转换为 pos。
     * 
     * @param permitForms permit forms
     * @return 结果列表
     */
    List<MenuPO> toPos(List<PermitForm> permitForms);

    /**
     * 用途：转换为 permit forms。
     * 
     * @param entities entities
     * @return 结果列表
     */
    List<PermitForm> toPermitForms(List<MenuPO> entities);

    /**
     * 用途：转换为 po。
     * 
     * @param menuForm menu form
     * @return 返回结果
     */
    MenuPO toPo(MenuForm menuForm);
}
