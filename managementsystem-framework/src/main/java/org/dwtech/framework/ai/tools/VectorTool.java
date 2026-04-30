package org.dwtech.framework.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.framework.ai.vector.store.CatalogVectorStoreService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
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
    private static final String ISBN_METADATA_KEY = "isbn";

    private final CatalogVectorStoreService catalogVectorStoreService;

    /**
     * 根据关键词在向量库中搜索图书，返回去重后的馆藏向量文档，供 AI 模型使用。
     *
     * @param keywords 搜索关键词列表
     * @return 去重后的馆藏向量文档列表
     */
    @Tool(description = "通过关键词搜索书库，返回去重后的馆藏向量文档，包含ISBN、书名、作者、简介等信息")
    public List<Document> searchVectors(List<String> keywords) {
        log.info("向量搜索关键词={}", keywords);
        List<Document> documents = catalogVectorStoreService.searchCatalogBookDocuments(keywords);
        log.info("向量搜索命中数量={} isbns={}", documents.size(), resolveHitIsbns(documents));
        return documents;
    }

    /**
     * 提取命中文档的 ISBN 用于日志记录，避免输出完整简介内容。
     *
     * @param documents 命中文档列表
     * @return ISBN 列表
     */
    private List<String> resolveHitIsbns(List<Document> documents) {
        return documents.stream()
                .map(this::resolveLogIsbn)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 优先使用元数据 ISBN，缺失时使用文档 ID 兜底。
     *
     * @param document 向量文档
     * @return 日志展示用 ISBN
     */
    private String resolveLogIsbn(Document document) {
        Object metadataIsbn = document.getMetadata().get(ISBN_METADATA_KEY);
        if (metadataIsbn instanceof String isbn && !isbn.isBlank()) {
            return isbn;
        }
        return document.getId();
    }
}
