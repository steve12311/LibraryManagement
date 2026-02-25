package org.dwtech.system.converter;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.UserBO;
import org.dwtech.common.model.Avatar;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.form.UserForm;
import org.dwtech.system.model.form.UserProfileForm;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.vo.CurrentUserVO;
import org.dwtech.system.model.vo.UserPageVO;
import org.dwtech.system.model.vo.UserProfileVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserPageVO toPageVo(UserBO bo);

    Page<UserPageVO> toPageVo(Page<UserBO> po);

    UserForm toForm(UserPO po);

    UserPO toPo(UserForm form);

    UserPO toPo(UserProfileForm form);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    CurrentUserVO toCurrentUser(UserPO po);

    @Mappings({
            @Mapping(target = "label", source = "nickname"),
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "avatar", source = "avatar", qualifiedByName = "toAvatar")
    })
    Option<String> toOption(UserPO po);

    @Named("toAvatar")
    default Avatar toAvatar(String avatar) {
        return new Avatar(avatar == null ? "" : avatar);
    }

    List<Option<String>> toOptions(List<UserPO> list);

    UserProfileVO toProfileVo(UserBO entity);
}
