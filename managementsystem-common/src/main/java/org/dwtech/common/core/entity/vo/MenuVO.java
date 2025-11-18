package org.dwtech.common.core.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MenuVO extends BaseVO{

    private Long id;

    private Long parentId;

    private String name;

    private Integer type;

    private String routeName;

    private String routePath;

    private String component;

    private Integer sort;

    private Integer visible;

    private String icon;

    private String redirect;

    private String perm;

    private List<MenuVO> children;

}