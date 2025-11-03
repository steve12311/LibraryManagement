package org.dwtech.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StockType {
    IN("in", "入库"), OUT("out", "出库");
    private final String code;
    private final String message;
}
