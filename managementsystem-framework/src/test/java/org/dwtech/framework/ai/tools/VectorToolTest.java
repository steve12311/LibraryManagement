package org.dwtech.framework.ai.tools;

import org.dwtech.framework.ai.vector.store.CatalogVectorStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorToolTest {

    @Mock
    private CatalogVectorStoreService catalogVectorStoreService;

    @Test
    void shouldReturnCatalogVectorDocumentsByKeywords() {
        List<String> keywords = List.of("Spring Boot", "入门");
        Document document = new Document("9787300000001", "Spring Boot 图书简介",
                Map.of("isbn", "9787300000001", "bookName", "Spring Boot 实战"));
        List<Document> documents = List.of(document);
        when(catalogVectorStoreService.searchCatalogBookDocuments(keywords)).thenReturn(documents);
        VectorTool vectorTool = new VectorTool(catalogVectorStoreService);

        List<Document> result = vectorTool.searchVectors(keywords);

        assertThat(result).containsExactly(document);
        verify(catalogVectorStoreService).searchCatalogBookDocuments(keywords);
    }
}
