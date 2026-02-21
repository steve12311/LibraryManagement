package org.dwtech.common.core.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lib_stock")
public class StockPO extends BasePO {
    private String isbn;
    private Integer stock;
    private Integer currentStock;
}
