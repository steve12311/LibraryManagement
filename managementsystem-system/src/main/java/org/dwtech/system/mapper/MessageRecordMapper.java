package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dwtech.system.model.entity.MessageRecordPO;

@Mapper
public interface MessageRecordMapper extends BaseMapper<MessageRecordPO> {
    /** 更新消息发送状态和错误信息（Consumer 调用，status: 1=SENT, 2=FAILED） */
    int updateStatusAndError(@Param("id") Long id, @Param("status") Integer status,
                             @Param("errorMsg") String errorMsg, @Param("retryCount") Integer retryCount);

    /**
     * 检查指定业务记录是否已发送过某类型的通知（去重用）
     */
    boolean existsByBizIdAndBizType(@Param("bizId") String bizId, @Param("bizType") String bizType);
}
