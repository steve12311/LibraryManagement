package org.dwtech.framework.ai.vector.queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogVectorSyncPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private CatalogVectorSyncProperties queueProperties;
    private CatalogVectorSyncPublisher catalogVectorSyncPublisher;

    @BeforeEach
    void setUp() {
        queueProperties = new CatalogVectorSyncProperties();
        queueProperties.setStreamKey("ai:catalog-vector:stream");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        catalogVectorSyncPublisher = new CatalogVectorSyncPublisher(redisTemplate, queueProperties);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void shouldPublishImmediatelyWhenNoTransactionExists() {
        catalogVectorSyncPublisher.publishAfterCommit(
                CatalogVectorSyncMessage.initial("9787300000001", CatalogVectorSyncTrigger.BOOK_UPDATED)
        );

        verify(streamOperations).add(any(MapRecord.class));
    }

    @Test
    void shouldDeferPublishingUntilAfterCommitWhenTransactionIsActive() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        catalogVectorSyncPublisher.publishAfterCommit(
                CatalogVectorSyncMessage.initial("9787300000001", CatalogVectorSyncTrigger.BOOK_UPDATED)
        );

        verify(streamOperations, never()).add(any(MapRecord.class));
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(streamOperations).add(any(MapRecord.class));
    }
}
