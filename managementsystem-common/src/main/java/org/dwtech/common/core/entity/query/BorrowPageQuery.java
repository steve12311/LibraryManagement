package org.dwtech.common.core.entity.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BorrowPageQuery extends BasePageQuery {
    private String field;
    private String keyword;
}
