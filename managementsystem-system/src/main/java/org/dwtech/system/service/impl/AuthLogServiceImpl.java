package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.mapper.AuthLogMapper;
import org.dwtech.system.model.entity.AuthLogPO;
import org.dwtech.system.service.AuthLogService;
import org.springframework.stereotype.Service;

/**
 * 认证日志服务实现
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Slf4j
@Service
public class AuthLogServiceImpl extends ServiceImpl<AuthLogMapper, AuthLogPO> implements AuthLogService {

    @Override
    public void saveQuietly(AuthLogPO authLog) {
        try {
            this.save(authLog);
        } catch (Exception e) {
            log.warn("认证日志落库失败, eventType={}, username={}, exceptionType={}",
                    authLog == null ? null : authLog.getEventType(),
                    authLog == null ? null : authLog.getUsername(),
                    e.getClass().getSimpleName());
        }
    }
}
