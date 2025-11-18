package org.dwtech.framework.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.service.MilvusService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AISearchService {
    private final DeepSeekChatModel deepSeekChatModel;
    private final OllamaEmbeddingModel ollamaEmbeddingModel;
    private final MilvusService milvusService;
    private final String sysText = """
            你是一个图书搜索专家，请根据用户的搜索意图，生成相关的搜索关键词列表。
                                用户想要搜索关于"%s"的书籍，请考虑以下方面：
                                1. 同义词和近义词
                                2. 相关技术栈和框架
                                3. 相关的概念和术语
                                4. 常见的书籍命名方式
            
                                请返回一个JSON数组，只包含10个相关的搜索关键词，不能多可以少，按相关性从高到低排序。
                                只返回JSON数组，不要有其他文字说明。
            
                                示例：["Java", "JDK", "JVM", "Spring", "Spring Boot", "Hibernate", "MyBatis", "Java编程", "Java核心", "多线程", "设计模式", "微服务", "分布式"]
            """;

    public Set<String> expandSearchKeywords(String originalQuery) {
        ChatResponse res = deepSeekChatModel.call(new Prompt(new SystemMessage(sysText), new UserMessage(originalQuery)));
        String jsonString = res.getResult().getOutput().getText();
        ObjectMapper mapper = new ObjectMapper();
        List<String> keywords;
        try {
            keywords = mapper.readValue(jsonString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(e.getMessage());
        }
        List<float[]> vector = new ArrayList<>();
        log.info(keywords.toString());
        keywords.forEach(keyword -> {
            EmbeddingResponse vectorData = ollamaEmbeddingModel.call(new EmbeddingRequest(
                    Collections.singletonList(keyword),
                    EmbeddingOptions.builder()
                            .build()
            ));
            vector.add(vectorData.getResult().getOutput());
        });

        return milvusService.searchVectors(vector);
    }

}
