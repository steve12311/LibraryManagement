package org.dwtech.system.service.impl;

import org.dwtech.system.mapper.RecommendationMapper;
import org.dwtech.system.model.bo.BookBorrowFreqBO;
import org.dwtech.system.model.bo.CoBorrowCountBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private Cursor<String> cursor;

    private RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(recommendationMapper, redisTemplate, redissonClient);
    }

    @Test
    void shouldReturnCachedRecommendationsFirst() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("cf:rec:1001", 0, 4L)).thenReturn(List.of("978-A", "978-B"));

        List<String> recommendations = recommendationService.getRecommendedIsbns(1001L, 5);

        assertThat(recommendations).containsExactly("978-A", "978-B");
        verifyNoInteractions(recommendationMapper, setOperations, hashOperations, redissonClient);
    }

    @Test
    void shouldAggregateSimilarBooksAndExcludeBorrowedBooks() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(listOperations.range("cf:rec:1001", 0, 9L)).thenReturn(List.of());
        when(setOperations.members("cf:user-books:1001")).thenReturn(Set.of());
        when(recommendationMapper.selectUserBorrowedIsbns(1001L)).thenReturn(List.of("978-A", "978-B"));
        when(hashOperations.entries("cf:sim:978-A")).thenReturn(Map.of("978-C", 0.8D, "978-D", 0.2D));
        when(hashOperations.entries("cf:sim:978-B")).thenReturn(Map.of("978-C", 0.4D, "978-A", 0.9D));

        List<String> recommendations = recommendationService.getRecommendedIsbns(1001L, 10);

        assertThat(recommendations).containsExactly("978-C", "978-D");
        verify(setOperations).add("cf:user-books:1001", "978-A", "978-B");
        verify(redisTemplate).expire("cf:user-books:1001", 5, TimeUnit.MINUTES);

        ArgumentCaptor<Collection<Object>> recommendationCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(listOperations).rightPushAll(eq("cf:rec:1001"), recommendationCaptor.capture());
        assertThat(recommendationCaptor.getValue()).containsExactly("978-C", "978-D");
        verify(redisTemplate).expire("cf:rec:1001", 30, TimeUnit.MINUTES);
    }

    @Test
    void shouldInvalidateRecommendationAndBorrowedBooksCache() {
        recommendationService.invalidateUserCache(1001L);

        verify(redisTemplate).delete("cf:rec:1001");
        verify(redisTemplate).delete("cf:user-books:1001");
    }

    @Test
    void shouldRebuildSimilarityMatrixWithCosineScore() throws InterruptedException {
        BookBorrowFreqBO bookA = new BookBorrowFreqBO();
        bookA.setIsbn("978-A");
        bookA.setBorrowerCount(4);
        BookBorrowFreqBO bookB = new BookBorrowFreqBO();
        bookB.setIsbn("978-B");
        bookB.setBorrowerCount(9);
        CoBorrowCountBO pair = new CoBorrowCountBO();
        pair.setIsbnA("978-A");
        pair.setIsbnB("978-B");
        pair.setCoBorrowCount(3);

        when(redissonClient.getLock("cf:rebuild:lock")).thenReturn(lock);
        when(lock.tryLock(0, 30, TimeUnit.MINUTES)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendationMapper.selectBookBorrowFrequency()).thenReturn(List.of(bookA, bookB));
        when(recommendationMapper.selectCoBorrowPairs()).thenReturn(List.of(pair));
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("cf:sim:old");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        recommendationService.rebuildSimilarityMatrix();

        verify(redisTemplate).delete("cf:sim:old");
        ArgumentCaptor<Map<Object, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq("cf:sim:978-A"), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsEntry("978-B", 0.5D);
        verify(hashOperations).putAll(eq("cf:sim:978-B"), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsEntry("978-A", 0.5D);
        verify(lock).unlock();
    }

    @Test
    void shouldSkipRebuildWhenLockNotAcquired() throws InterruptedException {
        when(redissonClient.getLock("cf:rebuild:lock")).thenReturn(lock);
        when(lock.tryLock(0, 30, TimeUnit.MINUTES)).thenReturn(false);

        recommendationService.rebuildSimilarityMatrix();

        verify(recommendationMapper, never()).selectBookBorrowFrequency();
        verify(lock, never()).unlock();
    }
}
