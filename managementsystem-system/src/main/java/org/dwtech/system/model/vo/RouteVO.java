package org.dwtech.system.model.vo;

import org.dwtech.common.base.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 菜单路由视图对象
 *
 * @author steve12311
 * @since 2025-11-18
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RouteVO extends BaseVO{

    private String path;

    private String component;

    private String redirect;

    private String name;

    private Meta meta;

    @Data
    public static class Meta {

        private String title;

        private String icon;

        private Boolean hidden;

        private Boolean keepAlive;

        private Boolean alwaysShow;

        private Map<String, String> params;
    }

    private List<RouteVO> children;
}
