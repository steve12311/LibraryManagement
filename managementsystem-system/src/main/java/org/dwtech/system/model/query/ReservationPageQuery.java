package org.dwtech.system.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BasePageQuery;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReservationPageQuery extends BasePageQuery {
    private String field;
    private String keyword;
}
