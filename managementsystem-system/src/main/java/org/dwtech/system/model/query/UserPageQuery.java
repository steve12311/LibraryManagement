package org.dwtech.system.model.query;

import org.dwtech.common.base.BasePageQuery;

import cn.hutool.db.sql.Direction;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.annontation.ValidField;

import java.util.List;

/**
 * 用户分页查询对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserPageQuery extends BasePageQuery {

    private String keywords;

    private Integer status;

    private Long deptId;

    private List<Long> roleIds;

    private List<String> createTime;

    @ValidField(allowedValues = {"create_time", "update_time"})
    private String field;

    private Direction direction;

    /**
     * 是否超级管理员
     */
    private Boolean isRoot;

}
