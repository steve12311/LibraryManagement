package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dwtech.common.constant.CacheConstants;
import org.dwtech.common.core.redis.RedisCache;
import org.dwtech.system.mapper.SysConfigMapper;
import org.dwtech.common.core.entity.SysConfig;
import org.dwtech.system.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl implements SysConfigService {
    private final SysConfigMapper sysConfigMapper;
    private final RedisCache redisCache;

    public SysConfigServiceImpl(SysConfigMapper sysConfigMapper, RedisCache redisCache) {
        this.sysConfigMapper = sysConfigMapper;
        this.redisCache = redisCache;
    }

    @Override
    public SysConfig selectConfigById(Long configId) {
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        return sysConfigMapper.selectOne(queryWrapper.eq("config_id", configId));
    }

    @Override
    public String selectConfigByKey(String key) {
        String cacheValue = redisCache.getCacheObject(getCacheKey(key));
        if (cacheValue != null) {
            return cacheValue;
        }
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        SysConfig config = sysConfigMapper.selectOne(queryWrapper.eq("config_key", key));
        if (config != null) {
            redisCache.setCacheObject(getCacheKey(key), config.getConfigValue());
            return config.getConfigValue();
        }
        return "";
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey) {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
