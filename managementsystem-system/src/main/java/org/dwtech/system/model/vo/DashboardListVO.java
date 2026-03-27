package org.dwtech.system.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 大屏分页区块
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
public class DashboardListVO<T> {
    private List<T> list;
    private long total;
}
