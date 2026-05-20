package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.MessageTemplatePO;

@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplatePO> {
    /** 按模板编码+渠道查找启用状态(status=1)的模板 */
    MessageTemplatePO selectByCodeAndChannel(@Param("templateCode") String templateCode,
                                             @Param("channel") String channel);
}
