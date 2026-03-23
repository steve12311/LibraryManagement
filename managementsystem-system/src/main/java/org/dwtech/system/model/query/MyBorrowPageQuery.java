package org.dwtech.system.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BasePageQuery;

/**
 * 当前登录用户借阅订单分页查询对象
 *
 * @author steve12311
 * @since 2026-03-23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyBorrowPageQuery extends BasePageQuery {

    @Min(value = 0, message = "状态值不合法")
    @Max(value = 2, message = "状态值不合法")
    private Integer status;
}
