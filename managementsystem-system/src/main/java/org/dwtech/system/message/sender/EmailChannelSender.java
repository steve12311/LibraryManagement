package org.dwtech.system.message.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.message.MessageChannelSender;
import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.model.enums.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件渠道发送器，通过 Spring Mail 的 {@link JavaMailSender} 投递纯文本邮件。
 * <p>
 * 发件地址由配置项 {@code message.email.from} 控制，默认 {@code noreply@library.com}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelSender implements MessageChannelSender {
    private final JavaMailSender mailSender;

    /** 发件人地址，可通过 application.yml 的 message.email.from 覆盖 */
    @Value("${message.email.from:noreply@library.com}")
    private String fromAddress;

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    /**
     * 构建并发送纯文本邮件。
     * 失败时抛出异常，由 {@code MessageStreamConsumer} 的重试机制处理。
     */
    @Override
    public void send(MessageRecordPO record) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(record.getRecipientAddress());
        message.setSubject(record.getSubject());
        message.setText(record.getContent());
        mailSender.send(message);
        log.info("action=send_email result=success userId={} to={}", record.getUserId(), record.getRecipientAddress());
    }
}
