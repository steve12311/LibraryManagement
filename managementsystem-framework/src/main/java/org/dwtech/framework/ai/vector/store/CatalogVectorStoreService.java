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
     * 用途：同步单本图书的向量文档。
     *
     * @param isbn isbn
     * @param bookName 图书名称
     * @param author 作者
     * @param intro 图书简介
     * 返回：无。
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
     * 用途：删除单本图书的向量文档。
     *
     * @param isbn isbn
     * 返回：无。
     */
    public void deleteCatalogBook(String isbn) {
        if (StrUtil.isBlank(isbn)) {
            return;
        }
        vectorStore.delete(List.of(isbn));
    }

    /**
     * 用途：根据关键词搜索相关图书 ISBN。
     *
     * @param keywords 关键词列表
     * @return 结果集合
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
                            .topK(1)
                            .build()
            );
            collectIsbns(isbns, documents);
        }
        return isbns;
    }

    /**
     * 用途：构建图书向量文档。
     *
     * @param isbn isbn
     * @param bookName 图书名称
     * @param author 作者
     * @param intro 图书简介
     * @return 返回结果
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
     * 用途：从搜索结果中收集 ISBN。
     *
     * @param isbns isbn 集合
     * @param documents 文档列表
     * 返回：无。
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
     * 用途：从文档中解析 ISBN。
     *
     * @param document 文档
     * @return 返回结果
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
