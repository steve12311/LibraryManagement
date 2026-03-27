package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseEntity;

/**
 * 认证日志实体
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_auth_log")
public class AuthLogPO extends BaseEntity {
    private String eventType;
    private Long userId;
    private String username;
    private String clientIp;
    private String sessionType;
    private Integer success;
    private String resultCode;
    private String failureSummary;
}
