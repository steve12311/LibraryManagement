package org.dwtech.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dwtech.common.base.BaseEntity;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_message_template")
/**
 * 消息模板实体，对应 sys_message_template。
 * 按 (templateCode, channel) 唯一确定一条模板。
 */
public class MessageTemplatePO extends BaseEntity {
    /** 模板编码，通常与 BizType 枚举值对应 */
    private String templateCode;
    /** 投递渠道（EMAIL / SMS） */
    private String channel;
    /** 模件主题模板（支持 ${var} 变量，SMS 时可为 null） */
    private String subject;
    /** 消息正文模板（支持 ${var} 变量） */
    private String contentTemplate;
    /** 模板描述（管理用） */
    private String description;
    /** 状态：1=启用, 0=禁用 */
    private Integer status;
}
