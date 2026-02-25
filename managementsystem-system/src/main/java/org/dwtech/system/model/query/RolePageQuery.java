package org.dwtech.system.model.query;

import org.dwtech.common.base.BasePageQuery;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色分页查询对象
 *
 */
@Getter
@Setter
public class RolePageQuery extends BasePageQuery {

    private String keywords;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
