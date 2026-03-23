package org.dwtech.common.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 基础分页请求对象
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Data
public class BasePageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于等于 1")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private int pageSize = 10;


}
