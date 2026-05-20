package org.dwtech.system.message.sender;

import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.message.MessageChannelSender;
import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.model.enums.Channel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 短信渠道发送器（当前为 Mock 实现）。
 * <p>
 * 默认启用（{@code message.sms.mock=true}）。接入真实短信网关时，
 * 将 {@code message.sms.mock} 设为 {@code false}，并提供新的实现类即可。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "message.sms", name = "mock", havingValue = "true", matchIfMissing = true)
public class SmsChannelSender implements MessageChannelSender {

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    /** Mock 实现：仅打印日志，不实际发送短信 */
    @Override
    public void send(MessageRecordPO record) {
        log.info("action=send_sms result=mock userId={} to={} content={}",
                record.getUserId(), record.getRecipientAddress(), record.getContent());
    }
}
