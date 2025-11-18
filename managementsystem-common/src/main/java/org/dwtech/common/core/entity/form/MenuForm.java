package org.dwtech.common.core.entity.form;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;

/**
 * 菜单表单对象
 */
@Data
public class MenuForm {

    private Long id;

    private Long parentId;

    private String name;

    private Integer type;

    private String routeName;

    private String routePath;

    private String component;

    private String perm;

    @Range(max = 1, min = 0, message = "显示状态不正确")
    private Integer visible;

    private Integer sort;

    private String icon;

    private String redirect;

    private Integer keepAlive;

    private Integer alwaysShow;

    private List<PermitForm> perms;

    // private List<KeyValue> params;

}
