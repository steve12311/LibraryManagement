package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseEntity;

/**
 * 操作审计日志实体
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class OperLogPO extends BaseEntity {
    private String module;
    private String action;
    private String bizResourceId;
    private Long operatorUserId;
    private String operatorUsername;
    private String requestMethod;
    private String requestPath;
    private String clientIp;
    private Integer success;
    private String resultCode;
    private String errorSummary;
}
