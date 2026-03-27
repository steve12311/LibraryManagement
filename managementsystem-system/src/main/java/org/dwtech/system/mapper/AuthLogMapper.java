package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dwtech.system.model.entity.AuthLogPO;

/**
 * 认证日志 Mapper
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Mapper
public interface AuthLogMapper extends BaseMapper<AuthLogPO> {
}
