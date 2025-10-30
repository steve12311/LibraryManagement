package org.dwtech.system.service;

import org.dwtech.common.core.entity.SysConfig;

public interface SysConfigService {
    SysConfig selectConfigById(Long configId);

    String selectConfigByKey(String key);
}
