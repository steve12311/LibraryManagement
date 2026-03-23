package org.dwtech.system.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.annontation.ValidField;
import org.dwtech.common.base.BasePageQuery;

/**
 * PublicBookPageQuery
 *
 * @author steve12311
 * @since 2026-03-23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PublicBookPageQuery extends BasePageQuery {
    @ValidField(allowedValues = {"name", "isbn", "author"})
    private String field;
    private String keyword;
}
