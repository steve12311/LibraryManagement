package org.dwtech.system.message;

import org.dwtech.system.model.enums.BizType;

import java.util.List;
import java.util.Map;

/**
 * 消息中心统一发送入口。
 * <p>
 * 所有业务通知（借阅到期、预约取书等）均通过此接口发送，不直接调用底层渠道。
 * 发送流程：查询用户偏好渠道 → 查模板 → 渲染内容 → 写入发送记录 → 推入 Redis Stream 异步投递。
 *
 * @author steve12311
 * @since 2026-05-20
 */
public interface MessageService {

    /**
     * 按用户偏好渠道发送消息。
     * <p>
     * 读取用户的 {@code notification_preference} JSON 字段，遍历所有启用的渠道逐一发送。
     * 适用于大多数业务场景（借阅提醒、预约通知等）。
     *
     * @param userId  接收用户 ID
     * @param bizType 业务类型枚举，决定使用哪套消息模板
     * @param bizId   关联的业务记录 ID（用于去重和追溯）
     * @param params  模板变量，如 bookName、dueDate 等
     */
    void send(Long userId, BizType bizType, String bizId, Map<String, String> params);

    /**
     * 指定单一渠道发送消息（忽略用户偏好）。
     * <p>
     * 用于管理员手动触发等需要强制走特定渠道的场景。
     *
     * @param userId  接收用户 ID
     * @param channel 渠道标识（"EMAIL" / "SMS"）
     * @param bizType 业务类型枚举
     * @param bizId   关联的业务记录 ID
     * @param params  模板变量
     */
    void send(Long userId, String channel, BizType bizType, String bizId, Map<String, String> params);

    /**
     * 批量发送消息（逐用户调用 {@link #send(Long, BizType, String, Map)}）。
     *
     * @param userIds 接收用户 ID 列表
     * @param bizType 业务类型枚举
     * @param bizId   关联的业务记录 ID
     * @param params  模板变量
     */
    void sendBatch(List<Long> userIds, BizType bizType, String bizId, Map<String, String> params);
}
