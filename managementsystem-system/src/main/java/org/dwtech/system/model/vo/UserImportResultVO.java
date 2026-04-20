package org.dwtech.system.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户导入结果视图对象
 *
 * @author steve12311
 * @since 2026-04-20
 */
@Data
public class UserImportResultVO {

    /**
     * 导入总条数（不含空白行与表头）
     */
    private int totalCount;

    /**
     * 导入成功条数
     */
    private int successCount;

    /**
     * 导入失败条数
     */
    private int failureCount;

    /**
     * 失败明细
     */
    private List<String> messages = new ArrayList<>();
}
