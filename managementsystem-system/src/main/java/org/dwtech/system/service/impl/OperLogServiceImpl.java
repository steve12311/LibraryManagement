package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.mapper.OperLogMapper;
import org.dwtech.system.model.entity.OperLogPO;
import org.dwtech.system.service.OperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Slf4j
@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLogPO> implements OperLogService {

    @Override
    public void saveQuietly(OperLogPO operLog) {
        try {
            this.save(operLog);
        } catch (Exception e) {
            log.warn("操作日志落库失败, module={}, action={}, exceptionType={}",
                    operLog == null ? null : operLog.getModule(),
                    operLog == null ? null : operLog.getAction(),
                    e.getClass().getSimpleName());
        }
    }
}
