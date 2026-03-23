package org.dwtech.system.model.query;

import org.dwtech.common.base.BasePageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.annontation.ValidField;
/**
 * PublishPageQuery
 *
 * @author steve12311
 * @since 2026-02-22
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class PublishPageQuery extends BasePageQuery {
    @ValidField(allowedValues = {"publishName", "address"})
    private String field;
    private String keyword;
}
