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
/**
 * UserConverter
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 用途：转换为 page vo。
     * 
     * @param bo bo
     * @return 返回结果
     */
    UserPageVO toPageVo(UserBO bo);

    /**
     * 用途：转换为 page vo。
     * 
     * @param po po
     * @return 分页结果
     */
    Page<UserPageVO> toPageVo(Page<UserBO> po);

    /**
     * 用途：转换为 form。
     * 
     * @param po po
     * @return 返回结果
     */
    UserForm toForm(UserPO po);

    /**
     * 用途：转换为 po。
     * 
     * @param form form
     * @return 返回结果
     */
    UserPO toPo(UserForm form);

    /**
     * 用途：转换为 po。
     * 
     * @param form form
     * @return 返回结果
     */
    UserPO toPo(UserProfileForm form);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    /**
     * 用途：转换为 current user。
     * 
     * @param po po
     * @return 返回结果
     */
    CurrentUserVO toCurrentUser(UserPO po);

    @Mappings({
            @Mapping(target = "label", source = "nickname"),
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "avatar", source = "avatar", qualifiedByName = "toAvatar")
    })
    /**
     * 用途：转换为 option。
     * 
     * @param po po
     * @return 返回结果
     */
    Option<String> toOption(UserPO po);

    /**
     * 用途：转换为 avatar。
     * 
     * @param avatar avatar
     * @return 返回结果
     */
    @Named("toAvatar")
    default Avatar toAvatar(String avatar) {
        return new Avatar(avatar == null ? "" : avatar);
    }

    /**
     * 用途：转换为 options。
     * 
     * @param list list
     * @return 结果列表
     */
    List<Option<String>> toOptions(List<UserPO> list);

    /**
     * 用途：转换为 profile vo。
     * 
     * @param entity entity
     * @return 返回结果
     */
    UserProfileVO toProfileVo(UserBO entity);
}
