package org.dwtech.common.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询请求基类
 * <p>
 * 所有分页查询的 Query 对象均应继承此类，自动获得页码与每页条数字段。
 * 前端无需手动拼接分页 SQL，由 MyBatis-Plus 的 {@code Page} 对象承载分页逻辑。
 * 默认第 1 页、每页 10 条，上限 100 条防止深分页拖垮数据库。
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class BasePageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 页码，从 1 开始，默认第 1 页 */
    @Min(value = 1, message = "页码必须大于等于 1")
    private int pageNum = 1;

    /** 每页条数，1-100，默认 10 条 */
    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private int pageSize = 10;

}
