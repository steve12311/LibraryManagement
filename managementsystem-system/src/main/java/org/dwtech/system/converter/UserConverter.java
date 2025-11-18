package org.dwtech.system.converter;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.core.entity.bo.UserBO;
import org.dwtech.common.core.entity.dto.Option;
import org.dwtech.common.core.entity.form.UserForm;
import org.dwtech.common.core.entity.po.UserPO;
import org.dwtech.common.core.entity.vo.CurrentUserVO;
import org.dwtech.common.core.entity.vo.UserPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserPageVO toPageVo(UserBO bo);

    Page<UserPageVO> toPageVo(Page<UserBO> po);

    UserForm toForm(UserPO po);

    UserPO toPO(UserForm form);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    CurrentUserVO toCurrentUser(UserPO po);

    @Mappings({
            @Mapping(target = "label", source = "nickname"),
            @Mapping(target = "value", source = "id")
    })
    Option<String> toOption(UserPO po);

    List<Option<String>> toOptions(List<UserPO> list);
}
