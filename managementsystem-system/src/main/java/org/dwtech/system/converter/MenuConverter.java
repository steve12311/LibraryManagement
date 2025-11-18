package org.dwtech.system.converter;

import org.dwtech.common.core.entity.form.MenuForm;
import org.dwtech.common.core.entity.form.PermitForm;
import org.dwtech.common.core.entity.po.MenuPO;
import org.dwtech.common.core.entity.vo.MenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MenuConverter {
    MenuVO toVo(MenuPO po);

    MenuForm toForm(MenuPO entity);

    @Mappings({
            @Mapping(target = "label", source = "name"),
            @Mapping(target = "value", source = "perm")
    })
    PermitForm toPermitForm(MenuPO entity);

    List<PermitForm> toPermitForms(List<MenuPO> entities);
}
