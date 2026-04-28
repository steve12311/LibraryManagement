package org.dwtech.system.converter;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.system.model.bo.UserBO;
import org.dwtech.common.model.Avatar;
import org.dwtech.common.model.Option;
import org.dwtech.system.model.dto.UserExportDTO;
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
 * 用户对象转换器（MapStruct），负责 Entity ↔ DTO ↔ VO 之间的映射
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * UserBO → UserPageVO
     */
    UserPageVO toPageVo(UserBO bo);

    /**
     * Page<UserBO> → Page<UserPageVO>
     */
    Page<UserPageVO> toPageVo(Page<UserBO> po);

    /**
     * UserBO → UserExportDTO
     *
     * @param gender 性别值
     * @param status 状态值
     */
    @Mappings({
            @Mapping(target = "genderLabel", source = "gender", qualifiedByName = "formatGenderLabel"),
            @Mapping(target = "statusLabel", source = "status", qualifiedByName = "formatStatusLabel")
    })
    UserExportDTO toExportDto(UserBO bo);

    /**
     * List<UserBO> → List<UserExportDTO>
     */
    List<UserExportDTO> toExportDtos(List<UserBO> list);

    /**
     * UserPO → UserForm
     */
    UserForm toForm(UserPO po);

    /**
     * UserForm → UserPO
     */
    UserPO toPo(UserForm form);

    /**
     * UserProfileForm → UserPO
     */
    UserPO toPo(UserProfileForm form);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    /**
     * UserPO → CurrentUserVO
     */
    CurrentUserVO toCurrentUser(UserPO po);

    @Mappings({
            @Mapping(target = "label", source = "nickname"),
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "avatar", source = "avatar", qualifiedByName = "toAvatar")
    })
    /**
     * UserPO → Option
     */
    Option<String> toOption(UserPO po);

    /**
     * String → Avatar
     */
    @Named("toAvatar")
    default Avatar toAvatar(String avatar) {
        return new Avatar(avatar == null ? "" : avatar);
    }

    /**
     * 将性别数值格式化为中文标签（1=男，2=女，0=保密）
     */
    @Named("formatGenderLabel")
    default String formatGenderLabel(Integer gender) {
        if (gender == null) {
            return "";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            case 0 -> "保密";
            default -> "";
        };
    }

    /**
     * 将状态数值格式化为中文标签（1=启用，其他=禁用）
     */
    @Named("formatStatusLabel")
    default String formatStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "启用" : "禁用";
    }

    /**
     * List<UserPO> → List<Option>
     */
    List<Option<String>> toOptions(List<UserPO> list);

    /**
     * UserBO → UserProfileVO
     */
    UserProfileVO toProfileVo(UserBO entity);
}
