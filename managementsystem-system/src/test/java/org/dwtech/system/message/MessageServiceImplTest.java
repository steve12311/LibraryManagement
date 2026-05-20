package org.dwtech.system.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dwtech.system.mapper.MessageRecordMapper;
import org.dwtech.system.mapper.MessageTemplateMapper;
import org.dwtech.system.message.queue.MessageStreamPublisher;
import org.dwtech.system.message.queue.MessageStreamMessage;
import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.model.entity.MessageTemplatePO;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.enums.BizType;
import org.dwtech.system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRecordMapper messageRecordMapper;
    @Mock
    private MessageTemplateMapper messageTemplateMapper;
    @Mock
    private MessageStreamPublisher messageStreamPublisher;
    @Mock
    private UserService userService;

    private MessageServiceImpl messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        messageService = new MessageServiceImpl(
                messageRecordMapper, messageTemplateMapper, messageStreamPublisher, userService, objectMapper);
    }

    @Test
    void shouldCreateRecordAndPublishForEmail() {
        UserPO user = new UserPO();
        user.setId(1001L);
        user.setEmail("user@example.com");
        user.setNotificationPreference("{\"email\":true,\"sms\":false}");
        when(userService.getById(1001L)).thenReturn(user);

        MessageTemplatePO template = new MessageTemplatePO();
        template.setTemplateCode("RESERVATION_READY");
        template.setSubject("预约到书通知");
        template.setContentTemplate("您预约的《${bookName}》已到馆");
        when(messageTemplateMapper.selectByCodeAndChannel("RESERVATION_READY", "email")).thenReturn(template);

        when(messageRecordMapper.insert(any(MessageRecordPO.class))).thenReturn(1);

        messageService.send(1001L, BizType.RESERVATION_READY, "42",
                Map.of("bookName", "Spring Boot 实战"));

        ArgumentCaptor<MessageRecordPO> recordCaptor = ArgumentCaptor.forClass(MessageRecordPO.class);
        verify(messageRecordMapper).insert(recordCaptor.capture());
        MessageRecordPO saved = recordCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1001L);
        assertThat(saved.getChannel()).isEqualTo("email");
        assertThat(saved.getBizType()).isEqualTo("RESERVATION_READY");
        assertThat(saved.getBizId()).isEqualTo("42");
        assertThat(saved.getSubject()).isEqualTo("预约到书通知");
        assertThat(saved.getContent()).isEqualTo("您预约的《Spring Boot 实战》已到馆");
        assertThat(saved.getRecipientAddress()).isEqualTo("user@example.com");
        assertThat(saved.getStatus()).isEqualTo(0);

        verify(messageStreamPublisher).publishAfterCommit(any(MessageStreamMessage.class));
    }

    @Test
    void shouldSendToBothChannelsWhenBothEnabled() {
        UserPO user = new UserPO();
        user.setId(1001L);
        user.setEmail("user@example.com");
        user.setMobile("13800138000");
        user.setNotificationPreference("{\"email\":true,\"sms\":true}");
        when(userService.getById(1001L)).thenReturn(user);

        MessageTemplatePO emailTemplate = new MessageTemplatePO();
        emailTemplate.setTemplateCode("RESERVATION_READY");
        emailTemplate.setSubject("预约到书通知");
        emailTemplate.setContentTemplate("您预约的《${bookName}》已到馆");
        when(messageTemplateMapper.selectByCodeAndChannel("RESERVATION_READY", "email")).thenReturn(emailTemplate);

        MessageTemplatePO smsTemplate = new MessageTemplatePO();
        smsTemplate.setTemplateCode("RESERVATION_READY");
        smsTemplate.setContentTemplate("【图书馆】您预约的《${bookName}》已到馆");
        when(messageTemplateMapper.selectByCodeAndChannel("RESERVATION_READY", "sms")).thenReturn(smsTemplate);

        when(messageRecordMapper.insert(any(MessageRecordPO.class))).thenReturn(1);

        messageService.send(1001L, BizType.RESERVATION_READY, "42",
                Map.of("bookName", "Spring Boot 实战"));

        ArgumentCaptor<MessageRecordPO> recordCaptor = ArgumentCaptor.forClass(MessageRecordPO.class);
        verify(messageRecordMapper, org.mockito.Mockito.times(2)).insert(recordCaptor.capture());
        assertThat(recordCaptor.getAllValues()).hasSize(2);
    }
}
