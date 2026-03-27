package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.system.model.entity.AuthLogPO;

/**
 * 认证日志服务
 *
 * @author steve12311
 * @since 2026-03-27
 */
public interface AuthLogService extends IService<AuthLogPO> {

    /**
     * 静默写入认证日志，失败时不影响主流程。
     *
     * @param authLog 认证日志
     */
    void saveQuietly(AuthLogPO authLog);
}
