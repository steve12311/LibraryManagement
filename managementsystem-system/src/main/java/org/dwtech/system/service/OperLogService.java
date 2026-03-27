package org.dwtech.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dwtech.system.model.entity.OperLogPO;

/**
 * 操作日志服务
 *
 * @author steve12311
 * @since 2026-03-27
 */
public interface OperLogService extends IService<OperLogPO> {

    /**
     * 静默写入操作日志，失败时不影响主流程。
     *
     * @param operLog 操作日志
     */
    void saveQuietly(OperLogPO operLog);
}
