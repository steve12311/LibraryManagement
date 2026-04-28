package org.dwtech.framework.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
/**
 * AI 智能检索服务 — 基于 DeepSeek + Tool Calling 的自然语言搜书
 * <p>
 * 调用流程：
 * <ol>
 *   <li>用户输入自然语言查询（如"推荐几本机器学习入门书"）</li>
 *   <li>构建 System Prompt（设定图书管理员角色 + 检索步骤约束）</li>
 *   <li>通过 Spring AI {@link ChatClient} 发送请求，携带自定义工具（日期、库存、向量检索）</li>
 *   <li>模型自主决定调用哪些 Tool，最终生成结构化推荐结果</li>
 *   <li>以 SSE 流式返回 {@code Flux<ChatResponse>}，前端逐步渲染</li>
 * </ol>
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AISearchService {
    private final DeepSeekChatModel deepSeekChatModel;
    private final ToolsLoader toolsLoader;
    private final ChatMemory chatMemory;

    /**
     * 执行 AI 增强检索
     * <p>
     * 构建 System/User Prompt → 创建 ChatClient（绑定对话记忆） → 加载 Tool List →
     * 以 SSE 流式返回回答，前端逐步渲染 Markdown。
     *
     * @param originalQuery  用户原始查询文本
     * @param conversationId 会话 ID，用于多轮对话记忆关联
     * @return SSE 流式响应
     */
    public Flux<ChatResponse> expandSearchKeywords(String originalQuery, Long conversationId) {
        SystemMessage sysPrompt = buildSystemPrompt();
        UserMessage userPrompt = buildUserPrompt(originalQuery);
        Prompt prompt = Prompt.builder()
                .messages(sysPrompt, userPrompt)
                .build();
        // 绑定对话记忆，支持多轮上下文
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        return chatClient
                .prompt(prompt)
                .tools(toolsLoader.getAllTools().toArray())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse();
    }

    /**
     * 构建 System Prompt，定义模型角色与行为约束。
     * 核心策略：强制模型调用向量工具核实馆藏后推荐，避免幻觉推荐不存在的书籍。
     */
    private SystemMessage buildSystemPrompt() {
        return SystemMessage.builder()
                .text("""
                        你是一个专业的图书馆智能助手，请根据用户的搜索需求推荐馆藏书籍。

                        请严格按照以下步骤为用户推荐书籍：

                        1. 深入理解用户需求：
                           - 分析用户想了解的主题领域
                           - 考虑不同的知识层次和学习目标
                           - 思考相关联的知识点和扩展阅读

                        2. 使用工具精准搜索：
                           - 必须调用向量搜索工具查询图书馆实际馆藏
                           - 重点查找用户感兴趣领域的具体书目
                           - 验证书籍在馆状态和可借阅情况

                        3. 提供专业推荐：
                           - 优先推荐馆藏中确实存在的书籍
                           - 按主题相关性、权威性和实用性排序
                           - 为每本书提供简要介绍和适合人群

                        4. 回答规范：
                           - 使用清晰的Markdown格式
                           - 按类别或难度分级展示推荐
                           - 标明具体书名、作者和简介
                           - 如相关书籍较少，可推荐相近主题

                        重要原则：
                        - 只推荐图书馆实际拥有的书籍
                        - 提供准确的馆藏信息和借阅建议
                        - 保持专业、耐心的服务态度
                        - 如无相关馆藏，请诚恳说明并建议替代方案

                        目标：帮助用户在图书馆找到最合适的阅读资源，提升学习和研究效率。
                        """)
                .build();
    }


    private UserMessage buildUserPrompt(String originalQuery) {
        return UserMessage.builder()
                .text(originalQuery)
                .build();
    }

}
