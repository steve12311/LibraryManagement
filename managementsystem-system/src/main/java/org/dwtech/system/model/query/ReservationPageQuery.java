package org.dwtech.system.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.annontation.ValidField;
import org.dwtech.common.base.BasePageQuery;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReservationPageQuery extends BasePageQuery {
    @ValidField(allowedValues = {"username", "isbn", "status"})
    private String field;
    private String keyword;
}
