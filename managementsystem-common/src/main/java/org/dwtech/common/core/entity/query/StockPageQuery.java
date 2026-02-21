package org.dwtech.common.core.entity.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.annontation.ValidField;

@EqualsAndHashCode(callSuper = true)
@Data
public class StockPageQuery extends BasePageQuery {
    @ValidField(allowedValues = {"name", "isbn", "author"})
    private String field;
    private String keyword;
}
