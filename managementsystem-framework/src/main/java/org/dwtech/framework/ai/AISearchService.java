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

@Slf4j
@Service
@RequiredArgsConstructor
public class AISearchService {
    private final DeepSeekChatModel deepSeekChatModel;
    private final ToolsLoader toolsLoader;
    private final ChatMemory chatMemory;

    public Flux<ChatResponse> expandSearchKeywords(String originalQuery, Long conversationId) {
        // 构建提示词
        SystemMessage sysPrompt = buildSystemPrompt();
        UserMessage userPrompt = buildUserPrompt(originalQuery);
        Prompt prompt = Prompt.builder()
                .messages(sysPrompt, userPrompt)
                .build();
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        // 调用模型
        return chatClient
                .prompt(prompt)
                .tools(toolsLoader.getAllTools().toArray(new Object[0]))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse();
    }

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
