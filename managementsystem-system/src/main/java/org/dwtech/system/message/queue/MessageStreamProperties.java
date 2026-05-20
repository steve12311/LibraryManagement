package org.dwtech.system.message.queue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 消息队列配置属性，对应 application.yml 中的 {@code message.queue.*}。
 *
 * @author steve12311
 * @since 2026-05-20
 */
@Data
@Validated
@ConfigurationProperties(prefix = "message.queue")
public class MessageStreamProperties {
    /** Redis Stream Key */
    @NotBlank
    private String streamKey = "message:send:stream";
    /** Consumer Group 名称，同一 Group 内消息只被消费一次 */
    @NotBlank
    private String consumerGroup = "message-consumer-group";
    /** 消费者实例名前缀，实际名称追加 :UUID */
    @NotBlank
    private String consumerNamePrefix = "message-consumer";
    /** 是否启用消费者（设为 false 可禁用异步消费） */
    private boolean consumerEnabled = true;
    /** 最大重试次数，超过后标记为 FAILED */
    @Min(1)
    private int maxRetries = 3;
    /** Stream 读取阻塞超时（秒） */
    @Min(1)
    private int pollTimeoutSeconds = 1;
}
