package org.dwtech.system.model.query;

import org.dwtech.common.base.BasePageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PublishPageQuery extends BasePageQuery {
    private String field;
    private String keyword;
}
