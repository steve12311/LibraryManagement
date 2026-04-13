package org.dwtech.framework.ai.vectorstore;

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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldResolveIsbnFromMetadataWhenSearchingCatalogBooks() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
                List.of(new Document("9787300000001", "Spring Boot 图书简介", Map.of("isbn", "9787300000001")))
        );

        Set<String> result = catalogVectorStoreService.searchCatalogBookIsbns(List.of("Spring Boot"));

        assertThat(result).containsExactly("9787300000001");
    }

    @Test
    void shouldFallbackToDocumentIdWhenMetadataIsbnMissing() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
                List.of(new Document("9787300000001", "Spring Boot 图书简介", Map.of("source", "catalog-book")))
        );

        Set<String> result = catalogVectorStoreService.searchCatalogBookIsbns(List.of("Spring Boot"));

        assertThat(result).containsExactly("9787300000001");
    }
}
