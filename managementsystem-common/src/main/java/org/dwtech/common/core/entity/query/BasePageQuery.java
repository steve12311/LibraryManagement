package org.dwtech.common.core.entity.query;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 基础分页请求对象
 */
@Data
public class BasePageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pageNum = 1;

    private int pageSize = 10;


}
