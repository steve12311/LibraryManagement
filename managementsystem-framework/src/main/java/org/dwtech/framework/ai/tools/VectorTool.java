package org.dwtech.framework.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.service.MilvusService;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.*;
/**
 * VectorTool
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorTool {
    private final OllamaEmbeddingModel ollamaEmbeddingModel;
    private final MilvusService milvusService;

    @Tool(description = "通过关键词搜索书库，返回相关书籍对应的ISBN码")
    public Set<String> searchVectors(List<String> keywords) {
        List<float[]> vectors = getVectors(keywords);
        return milvusService.searchVectors(vectors);
    }

    public List<float[]> getVectors(List<String> keywords) {
        log.info("关键词{}", keywords.toString());
        List<float[]> vector = new ArrayList<>();
        keywords.forEach(keyword -> {
            EmbeddingResponse vectorData = ollamaEmbeddingModel.call(new EmbeddingRequest(
                    Collections.singletonList(keyword),
                    EmbeddingOptions.builder()
                            .build()
            ));
            vector.add(vectorData.getResult().getOutput());
        });
        log.info("词向量组{}", vector);
        return vector;
    }
}
