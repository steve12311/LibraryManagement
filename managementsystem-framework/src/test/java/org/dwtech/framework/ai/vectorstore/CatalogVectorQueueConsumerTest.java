package org.dwtech.framework.ai.vectorstore;

import org.dwtech.system.model.form.BookForm;
import org.dwtech.system.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogVectorQueueConsumerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private BookService bookService;

    @Mock
    private CatalogVectorStoreService catalogVectorStoreService;

    @Mock
    private CatalogVectorQueuePublisher catalogVectorQueuePublisher;

    @Mock
    private org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object> record;

    private CatalogVectorQueueConsumer catalogVectorQueueConsumer;
    private CatalogVectorQueueProperties queueProperties;

    @BeforeEach
    void setUp() {
        queueProperties = new CatalogVectorQueueProperties();
        queueProperties.setStreamKey("ai:catalog-vector:stream");
        queueProperties.setConsumerGroup("catalog-vector-group");
        queueProperties.setMaxRetries(3);
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        catalogVectorQueueConsumer = new CatalogVectorQueueConsumer(
                redisTemplate,
                queueProperties,
                bookService,
                catalogVectorStoreService,
                catalogVectorQueuePublisher
        );
    }

    @Test
    void shouldSyncCatalogBookUsingDatabaseSnapshot() {
        BookForm bookForm = new BookForm();
        bookForm.setIsbn("9787300000001");
        bookForm.setName("Spring Boot 实战");
        bookForm.setAuthor("张三");
        bookForm.setIntro("图书简介");
        when(bookService.getBookByIsbn("9787300000001")).thenReturn(bookForm);

        catalogVectorQueueConsumer.handleMessage(
                CatalogVectorQueueMessage.initial("9787300000001", CatalogVectorQueueTrigger.BOOK_UPDATED)
        );

        verify(catalogVectorStoreService).syncCatalogBook("9787300000001", "Spring Boot 实战", "张三", "图书简介");
    }

    @Test
    void shouldDeleteVectorWhenBookMissingOrIntroBlank() {
        when(bookService.getBookByIsbn("9787300000001")).thenReturn(null);

        catalogVectorQueueConsumer.handleMessage(
                CatalogVectorQueueMessage.initial("9787300000001", CatalogVectorQueueTrigger.BOOK_UPDATED)
        );

        verify(catalogVectorStoreService).deleteCatalogBook("9787300000001");
    }

    @Test
    void shouldRepublishRetryAndAcknowledgeOriginalRecordWhenHandlingFails() {
        when(bookService.getBookByIsbn("9787300000001")).thenThrow(new IllegalStateException("boom"));
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);
        when(record.getValue()).thenReturn(Map.of(
                "isbn", "9787300000001",
                "trigger", "BOOK_UPDATED",
                "retryCount", "0",
                "occurredAt", "2026-04-13T00:00:00Z"
        ));

        catalogVectorQueueConsumer.handleRecord(record);

        verify(catalogVectorQueuePublisher).publishNow(
                new CatalogVectorQueueMessage("9787300000001", CatalogVectorQueueTrigger.BOOK_UPDATED, 1, "2026-04-13T00:00:00Z")
        );
        verify(streamOperations).acknowledge("ai:catalog-vector:stream", "catalog-vector-group", recordId);
    }

    @Test
    void shouldAcknowledgeAndSkipWhenMessageFieldsInvalid() {
        RecordId recordId = RecordId.of("2-0");
        when(record.getId()).thenReturn(recordId);
        when(record.getValue()).thenReturn(Map.of("isbn", "", "trigger", "BOOK_UPDATED", "retryCount", "0"));

        catalogVectorQueueConsumer.handleRecord(record);

        verify(streamOperations).acknowledge("ai:catalog-vector:stream", "catalog-vector-group", recordId);
        verify(catalogVectorStoreService, never()).syncCatalogBook(any(), any(), any(), any());
    }
}
