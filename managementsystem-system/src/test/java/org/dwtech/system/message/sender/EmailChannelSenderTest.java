package org.dwtech.system.message.sender;

import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.model.enums.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailChannelSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailChannelSender sender;

    @BeforeEach
    void setUp() {
        sender = new EmailChannelSender(mailSender);
        ReflectionTestUtils.setField(sender, "fromAddress", "noreply@library.com");
    }

    @Test
    void shouldReturnEmailChannel() {
        assertThat(sender.channel()).isEqualTo(Channel.EMAIL);
    }

    @Test
    void shouldSendEmailWithCorrectFields() {
        MessageRecordPO record = new MessageRecordPO();
        record.setSubject("Test Subject");
        record.setContent("Test Content");
        record.setUserId(1001L);
        record.setRecipientAddress("user@example.com");

        sender.send(record);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@library.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Test Subject");
        assertThat(message.getText()).isEqualTo("Test Content");
    }
}
