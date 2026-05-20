package org.dwtech.system.message;

import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.model.enums.Channel;

/**
 * 消息渠道发送器策略接口（策略模式）。
 * <p>
 * 每个实现类负责一种投递渠道（邮件、短信等），由 {@code MessageStreamConsumer}
 * 根据消息记录的 {@code channel} 字段动态选择对应的发送器。
 * <p>
 * 新增渠道只需：1) 实现此接口；2) 加 {@code @Component} 注解即可自动注册。
 *
 * @author steve12311
 * @since 2026-05-20
 */
public interface MessageChannelSender {

    /**
     * 返回此发送器负责的渠道标识。
     */
    Channel channel();

    /**
     * 执行实际的消息投递。
     *
     * @param record 已持久化的消息记录，包含收件地址、内容、主题等
     * @throws RuntimeException 投递失败时抛出，由调用方决定重试或标记失败
     */
    void send(MessageRecordPO record);
}
