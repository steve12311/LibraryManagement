package org.dwtech.framework.ai.vector.store;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CatalogVectorStoreService
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogVectorStoreService {
    private static final String ISBN_METADATA_KEY = "isbn";
    private static final String BOOK_NAME_METADATA_KEY = "bookName";
    private static final String AUTHOR_METADATA_KEY = "author";
    private static final String SOURCE_METADATA_KEY = "source";
    private static final String CATALOG_SOURCE = "catalog-book";

    private final VectorStore vectorStore;

    /**
     * 删除原有向量文档并重新添加，实现单本图书的向量文档同步。
     *
     * @param isbn     图书 ISBN
     * @param bookName 图书名称
     * @param author   作者
     * @param intro    图书简介
     */
    public void syncCatalogBook(String isbn, String bookName, String author, String intro) {
        if (StrUtil.isBlank(isbn)) {
            throw new BusinessException(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY, "AI 向量同步缺少 ISBN");
        }
        if (StrUtil.isBlank(intro)) {
            throw new BusinessException(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY, "AI 向量同步缺少图书简介");
        }
        vectorStore.delete(List.of(isbn));
        vectorStore.add(List.of(buildCatalogDocument(isbn, bookName, author, intro)));
    }

    /**
     * 根据 ISBN 从向量库中删除对应图书的向量文档。
     *
     * @param isbn 图书 ISBN
     */
    public void deleteCatalogBook(String isbn) {
        if (StrUtil.isBlank(isbn)) {
            return;
        }
        vectorStore.delete(List.of(isbn));
    }

    /**
     * 根据关键词在向量库中搜索相似图书，返回匹配的 ISBN 集合（已去重）。
     *
     * @param keywords 关键词列表
     * @return 匹配图书的 ISBN 集合
     */
    public Set<String> searchCatalogBookIsbns(List<String> keywords) {
        Set<String> isbns = new LinkedHashSet<>();
        if (keywords == null || keywords.isEmpty()) {
            return isbns;
        }
        for (String keyword : keywords) {
            if (StrUtil.isBlank(keyword)) {
                continue;
            }
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(keyword)
                            .topK(5)
                            .build()
            );
            collectIsbns(isbns, documents);
        }
        return isbns;
    }

    /**
     * 构建包含图书元数据和简介的向量文档对象。
     *
     * @param isbn     图书 ISBN
     * @param bookName 图书名称
     * @param author   作者
     * @param intro    图书简介
     * @return 向量文档对象
     */
    private Document buildCatalogDocument(String isbn, String bookName, String author, String intro) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ISBN_METADATA_KEY, isbn);
        metadata.put(SOURCE_METADATA_KEY, CATALOG_SOURCE);
        if (StrUtil.isNotBlank(bookName)) {
            metadata.put(BOOK_NAME_METADATA_KEY, bookName);
        }
        if (StrUtil.isNotBlank(author)) {
            metadata.put(AUTHOR_METADATA_KEY, author);
        }
        return new Document(isbn, intro, metadata);
    }

    /**
     * 遍历向量搜索结果，提取并收集所有有效的 ISBN。
     *
     * @param isbns     用于收集 ISBN 的集合
     * @param documents 文档列表
     */
    private void collectIsbns(Set<String> isbns, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (Document document : documents) {
            String isbn = resolveIsbn(document);
            if (StrUtil.isBlank(isbn)) {
                log.warn("action=search_catalog_vector result=skipped reason=missing_isbn_metadata documentId={}", document.getId());
                continue;
            }
            isbns.add(isbn);
        }
    }

    /**
     * 从向量文档的元数据中提取 ISBN，兜底使用文档 ID。
     *
     * @param document 向量文档
     * @return ISBN 字符串，无法解析时返回 null
     */
    private String resolveIsbn(Document document) {
        Object metadataIsbn = document.getMetadata().get(ISBN_METADATA_KEY);
        if (metadataIsbn instanceof String isbn && StrUtil.isNotBlank(isbn)) {
            return isbn;
        }
        if (StrUtil.isNotBlank(document.getId())) {
            log.warn("action=search_catalog_vector result=fallback_to_document_id documentId={}", document.getId());
            return document.getId();
        }
        return null;
    }
}
