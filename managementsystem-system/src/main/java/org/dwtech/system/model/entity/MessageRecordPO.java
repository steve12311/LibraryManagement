package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseEntity;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_message_record")
/**
 * 消息发送记录实体，对应 sys_message_record。
 * 每次发送尝试（含重试）都会生成一条记录。
 */
public class MessageRecordPO extends BaseEntity {
    /** 业务类型（OVERDUE / RESERVATION_READY 等） */
    private String bizType;
    /** 关联的业务记录 ID（如借阅 ID、预约 ID） */
    private String bizId;
    /** 接收用户 ID */
    private Long userId;
    /** 投递渠道（EMAIL / SMS） */
    private String channel;
    /** 使用的消息模板编码 */
    private String templateCode;
    /** 渲染后的邮件主题（SMS 时可为 null） */
    private String subject;
    /** 渲染后的消息正文 */
    private String content;
    /** 收件地址（邮箱或手机号） */
    private String recipientAddress;
    /** 发送状态：0=待发送, 1=已发送, 2=发送失败 */
    private Integer status;
    /** 已重试次数 */
    private Integer retryCount;
    /** 最近一次失败的错误信息 */
    private String errorMsg;
}
