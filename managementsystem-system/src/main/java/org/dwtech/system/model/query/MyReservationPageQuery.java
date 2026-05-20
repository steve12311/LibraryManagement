package org.dwtech.system.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BasePageQuery;

@EqualsAndHashCode(callSuper = true)
@Data
public class MyReservationPageQuery extends BasePageQuery {
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 4, message = "状态值不合法")
    private Integer status;
}
