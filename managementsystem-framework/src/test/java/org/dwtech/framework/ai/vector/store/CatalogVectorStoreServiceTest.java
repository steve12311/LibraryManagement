package org.dwtech.framework.ai.vector.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogVectorStoreServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private CatalogVectorStoreService catalogVectorStoreService;

    @Test
    void shouldDeleteOldDocumentAndAddNewDocumentWhenSyncingCatalogBook() {
        catalogVectorStoreService.syncCatalogBook("9787300000001", "Spring Boot 实战", "张三", "Spring Boot 图书简介");

        verify(vectorStore).delete(List.of("9787300000001"));
        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());
        Document document = documentsCaptor.getValue().getFirst();
        assertThat(document.getId()).isEqualTo("9787300000001");
        assertThat(document.getText()).isEqualTo("Spring Boot 图书简介");
        assertThat(document.getMetadata()).containsEntry("isbn", "9787300000001");
        assertThat(document.getMetadata()).containsEntry("bookName", "Spring Boot 实战");
        assertThat(document.getMetadata()).containsEntry("author", "张三");
        assertThat(document.getMetadata()).containsEntry("source", "catalog-book");
    }

    @Test
    void shouldDeleteCatalogBookByIsbn() {
        catalogVectorStoreService.deleteCatalogBook("9787300000001");

        verify(vectorStore).delete(List.of("9787300000001"));
    }

    @Test
    void shouldReturnDocumentResolvedByMetadataIsbnWhenSearchingCatalogBooks() {
        Document document = new Document("doc-1", "Spring Boot 图书简介", Map.of("isbn", "9787300000001"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
                List.of(document)
        );

        List<Document> result = catalogVectorStoreService.searchCatalogBookDocuments(List.of("Spring Boot"));

        assertThat(result).containsExactly(document);
    }

    @Test
    void shouldDeduplicateDocumentsByIsbnAndKeepFirstHit() {
        Document first = new Document("doc-1", "第一本简介", Map.of("isbn", "9787300000001"));
        Document second = new Document("doc-2", "第二本简介", Map.of("isbn", "9787300000002"));
        Document duplicate = new Document("doc-3", "重复命中简介", Map.of("isbn", "9787300000001"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(first, second), List.of(duplicate));

        List<Document> result = catalogVectorStoreService.searchCatalogBookDocuments(List.of("Spring Boot", "Java"));

        assertThat(result).containsExactly(first, second);
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void shouldFallbackToDocumentIdWhenMetadataIsbnMissing() {
        Document document = new Document("9787300000001", "Spring Boot 图书简介", Map.of("source", "catalog-book"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
                List.of(document)
        );

        List<Document> result = catalogVectorStoreService.searchCatalogBookDocuments(List.of("Spring Boot"));

        assertThat(result).containsExactly(document);
    }

    @Test
    void shouldSkipDocumentWhenIsbnCannotBeResolved() {
        Document document = mock(Document.class);
        when(document.getMetadata()).thenReturn(Map.of("source", "catalog-book"));
        when(document.getId()).thenReturn("");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
                List.of(document)
        );

        List<Document> result = catalogVectorStoreService.searchCatalogBookDocuments(List.of("Spring Boot"));

        assertThat(result).isEmpty();
    }
}
