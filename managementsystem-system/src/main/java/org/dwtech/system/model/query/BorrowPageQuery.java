package org.dwtech.system.model.query;

import org.dwtech.common.base.BasePageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.annontation.ValidField;
/**
 * BorrowPageQuery
 *
 * @author steve12311
 * @since 2026-02-22
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class BorrowPageQuery extends BasePageQuery {
    @ValidField(allowedValues = {"status", "username", "isbn"})
    private String field;
    private String keyword;
}
