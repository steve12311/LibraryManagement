package org.dwtech.system.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.mapper.MessageRecordMapper;
import org.dwtech.system.mapper.MessageTemplateMapper;
import org.dwtech.system.message.queue.MessageStreamMessage;
import org.dwtech.system.message.queue.MessageStreamPublisher;
import org.dwtech.system.model.entity.MessageRecordPO;
import org.dwtech.system.model.entity.MessageTemplatePO;
import org.dwtech.system.model.entity.UserPO;
import org.dwtech.system.model.enums.BizType;
import org.dwtech.system.model.enums.Channel;
import org.dwtech.system.model.enums.MessageStatus;
import org.dwtech.system.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息中心核心服务实现。
 * <p>
 * <b>发送流程</b>（以 {@link #send(Long, BizType, String, Map)} 为例）：
 * <ol>
 *   <li>查询用户信息，不存在则跳过</li>
 *   <li>解析用户的 {@code notification_preference} JSON，得到启用的渠道列表</li>
 *   <li>对每个启用的渠道，调用 {@link #sendToChannel}：
 *     <ol>
 *       <li>根据 bizType + channel 查找消息模板（{@code sys_message_template}）</li>
 *       <li>用正则 {@code ${key}} 渲染模板变量</li>
 *       <li>解析收件地址（EMAIL → email 字段，SMS → mobile 字段）</li>
 *       <li>构建 {@link MessageRecordPO} 并写入数据库（状态=PENDING）</li>
 *       <li>事务提交后通过 {@link MessageStreamPublisher} 推入 Redis Stream</li>
 *     </ol>
 *   </li>
 * </ol>
 * <p>
 * 实际的渠道投递由 {@code MessageStreamConsumer} 异步完成，本服务只负责"写记录 + 入队"。
 *
 * @author steve12311
 * @since 2026-05-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /** 模板变量正则，匹配 ${variableName} 格式 */
    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private final MessageRecordMapper messageRecordMapper;
    private final MessageTemplateMapper messageTemplateMapper;
    private final MessageStreamPublisher messageStreamPublisher;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * 按用户偏好渠道发送消息。
     * <p>
     * 步骤：
     * 1. 查询用户，不存在则 warn 日志并返回
     * 2. 解析 notification_preference JSON → Map&lt;channel, enabled&gt;
     * 3. 遍历 enabled=true 的渠道，逐一调用 sendToChannel
     */
    @Override
    @Transactional
    public void send(Long userId, BizType bizType, String bizId, Map<String, String> params) {
        UserPO user = userService.getById(userId);
        if (user == null) {
            log.warn("action=message_send result=skipped reason=user_not_found userId={}", userId);
            return;
        }
        Map<String, Boolean> preference = parsePreference(user.getNotificationPreference());
        for (Map.Entry<String, Boolean> entry : preference.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                sendToChannel(userId, entry.getKey(), bizType, bizId, params, user);
            }
        }
    }

    /**
     * 指定单一渠道发送消息（跳过用户偏好解析）。
     */
    @Override
    @Transactional
    public void send(Long userId, String channel, BizType bizType, String bizId, Map<String, String> params) {
        UserPO user = userService.getById(userId);
        if (user == null) {
            log.warn("action=message_send result=skipped reason=user_not_found userId={}", userId);
            return;
        }
        sendToChannel(userId, channel, bizType, bizId, params, user);
    }

    /**
     * 批量发送：逐用户调用偏好渠道发送。
     */
    @Override
    @Transactional
    public void sendBatch(List<Long> userIds, BizType bizType, String bizId, Map<String, String> params) {
        for (Long userId : userIds) {
            send(userId, bizType, bizId, params);
        }
    }

    /**
     * 向指定渠道发送一条消息（核心私有方法）。
     * <p>
     * 步骤：
     * 1. 按 bizType + channel 查模板，不存在则跳过
     * 2. 渲染模板内容（${key} → 实际值）
     * 3. 根据渠道类型解析收件地址（EMAIL→email, SMS→mobile）
     * 4. 构建 MessageRecordPO（状态=PENDING, retryCount=0）并 INSERT
     * 5. 事务提交后推入 Redis Stream（保证记录已落盘再入队）
     */
    private void sendToChannel(Long userId, String channel, BizType bizType, String bizId,
                                Map<String, String> params, UserPO user) {
        // Step 1: 查找模板
        MessageTemplatePO template = messageTemplateMapper.selectByCodeAndChannel(
                bizType.getValue(), channel);
        if (template == null) {
            log.warn("action=message_send result=skipped reason=template_not_found bizType={} channel={}",
                    bizType.getValue(), channel);
            return;
        }
        // Step 2: 渲染模板变量
        String content = renderTemplate(template.getContentTemplate(), params);
        String subject = template.getSubject() != null ? renderTemplate(template.getSubject(), params) : null;
        // Step 3: 解析收件地址
        String recipientAddress = resolveRecipientAddress(user, channel);

        // Step 4: 持久化消息记录
        MessageRecordPO record = new MessageRecordPO();
        record.setUserId(userId);
        record.setChannel(channel);
        record.setBizType(bizType.getValue());
        record.setBizId(bizId);
        record.setTemplateCode(template.getTemplateCode());
        record.setSubject(subject);
        record.setContent(content);
        record.setRecipientAddress(recipientAddress);
        record.setStatus(MessageStatus.PENDING.getValue());
        record.setRetryCount(0);
        messageRecordMapper.insert(record);

        // Step 5: 事务提交后入队（保证记录已落盘，Consumer 可查到）
        messageStreamPublisher.publishAfterCommit(MessageStreamMessage.initial(record.getId()));
        log.info("action=message_send result=queued recordId={} userId={} channel={} bizType={}",
                record.getId(), userId, channel, bizType.getValue());
    }

    /**
     * 渲染模板字符串：将 ${key} 替换为 params 中对应的值。
     * 未匹配的变量保留原样（不替换）。
     */
    private String renderTemplate(String template, Map<String, String> params) {
        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = params.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 根据渠道类型解析收件地址：EMAIL → user.email，SMS → user.mobile。
     */
    private String resolveRecipientAddress(UserPO user, String channel) {
        if (Channel.EMAIL.getValue().equalsIgnoreCase(channel)) {
            return user.getEmail();
        } else if (Channel.SMS.getValue().equalsIgnoreCase(channel)) {
            return user.getMobile();
        }
        return null;
    }

    /**
     * 解析用户的 notification_preference JSON 字段。
     * 格式示例：{@code {"email":true,"sms":false}}
     * 解析失败时降级为默认值（email=true, sms=false）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Boolean> parsePreference(String json) {
        if (json == null || json.isBlank()) {
            return Map.of(Channel.EMAIL.getValue(), true, Channel.SMS.getValue(), false);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("action=parse_preference result=fallback reason=invalid_json json={}", json);
            return Map.of(Channel.EMAIL.getValue(), true, Channel.SMS.getValue(), false);
        }
    }
}
