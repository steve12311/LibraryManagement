package org.dwtech.system.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.system.mapper.RecommendationMapper;
import org.dwtech.system.model.bo.BookBorrowFreqBO;
import org.dwtech.system.model.bo.CoBorrowCountBO;
import org.dwtech.system.service.RecommendationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于物品的协同过滤推荐服务实现
 *
 * @author steve12311
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private static final int TOP_SIMILARITIES_PER_BOOK = 20;
    private static final long USER_RECOMMENDATION_TTL_MINUTES = 30;
    private static final long USER_BOOKS_TTL_MINUTES = 5;
    private static final long REBUILD_LOCK_LEASE_MINUTES = 30;

    private final RecommendationMapper recommendationMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    @Override
    public List<String> getRecommendedIsbns(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }

        // 第一步：查缓存
        String recommendationKey = StrUtil.format(RedisConstants.Cf.USER_RECS, userId);
        List<String> cachedRecommendations = getCachedRecommendations(recommendationKey, limit);
        if (!cachedRecommendations.isEmpty()) {
            return cachedRecommendations;
        }

        // 第二步：获取用户借阅史（冷启动用户无借阅记录则返回空）
        List<String> borrowedIsbns = getUserBorrowedIsbns(userId);
        if (borrowedIsbns.isEmpty()) {
            return Collections.emptyList();
        }

        // 第三步：遍历用户借过的每本书，从 Redis 相似度 Hash 聚合候选推荐
        Set<String> borrowedSet = new HashSet<>(borrowedIsbns);
        Map<String, Double> candidateScores = new HashMap<>();
        for (String borrowedIsbn : borrowedIsbns) {
            Map<Object, Object> similarities = redisTemplate.opsForHash()
                    .entries(StrUtil.format(RedisConstants.Cf.SIMILARITY, borrowedIsbn));
            similarities.forEach((similarIsbn, score) -> {
                String isbn = Objects.toString(similarIsbn, "");
                if (isbn.isBlank() || borrowedSet.contains(isbn)) {
                    return;
                }
                candidateScores.merge(isbn, toDouble(score), Double::sum);
            });
        }

        // 第四步：按聚合分数降序排序，取 Top-N，写入缓存
        List<String> recommendations = sortByScore(candidateScores, limit);
        cacheRecommendations(recommendationKey, recommendations);
        return recommendations;
    }

    @Override
    public void invalidateUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(StrUtil.format(RedisConstants.Cf.USER_RECS, userId));
        redisTemplate.delete(StrUtil.format(RedisConstants.Cf.USER_BOOKS, userId));
    }

    @Override
    public void rebuildSimilarityMatrix() {
        RLock lock = redissonClient.getLock(RedisConstants.Cf.REBUILD_LOCK);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, REBUILD_LOCK_LEASE_MINUTES, TimeUnit.MINUTES);
            if (!locked) {
                log.info("协同过滤相似度矩阵正在重建，本次跳过");
                return;
            }
            doRebuildSimilarityMatrix();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("协同过滤相似度矩阵重建被中断", ex);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 重建核心流程：
     * <ol>
     *   <li>加载每本图书的去重借阅人数（用于余弦归一化分母）</li>
     *   <li>查询共借对（达到最小支持度阈值）</li>
     *   <li>计算余弦相似度：coBorrowCount / sqrt(freqA × freqB)</li>
     *   <li>双向写入相似度矩阵，取 Top-20 存入 Redis Hash</li>
     * </ol>
     */
    private void doRebuildSimilarityMatrix() {
        Map<String, Integer> borrowerFrequency = loadBorrowerFrequency();
        List<CoBorrowCountBO> coBorrowPairs = recommendationMapper.selectCoBorrowPairs();
        Map<String, Map<String, Double>> similarityMatrix = new HashMap<>();

        for (CoBorrowCountBO pair : coBorrowPairs) {
            Integer countA = borrowerFrequency.get(pair.getIsbnA());
            Integer countB = borrowerFrequency.get(pair.getIsbnB());
            if (countA == null || countB == null || countA == 0 || countB == 0) {
                continue;
            }
            double similarity = pair.getCoBorrowCount() / Math.sqrt(countA.doubleValue() * countB.doubleValue());
            similarityMatrix.computeIfAbsent(pair.getIsbnA(), key -> new HashMap<>()).put(pair.getIsbnB(), similarity);
            similarityMatrix.computeIfAbsent(pair.getIsbnB(), key -> new HashMap<>()).put(pair.getIsbnA(), similarity);
        }

        replaceSimilarityCache(similarityMatrix);
        log.info("协同过滤相似度矩阵重建完成，图书数：{}，共借对数：{}", similarityMatrix.size(), coBorrowPairs.size());
    }

    private Map<String, Integer> loadBorrowerFrequency() {
        List<BookBorrowFreqBO> frequencies = recommendationMapper.selectBookBorrowFrequency();
        Map<String, Integer> frequencyMap = new HashMap<>(frequencies.size());
        for (BookBorrowFreqBO frequency : frequencies) {
            frequencyMap.put(frequency.getIsbn(), frequency.getBorrowerCount());
        }
        return frequencyMap;
    }

    /** 用 SCAN 替代 KEYS 避免生产环境阻塞，逐个删除旧相似度键 */
    private void replaceSimilarityCache(Map<String, Map<String, Double>> similarityMatrix) {
        String pattern = RedisConstants.Cf.SIMILARITY.replace("{}", "*");
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern).count(100).build())) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }

        similarityMatrix.forEach((isbn, similarities) -> {
            Map<Object, Object> topSimilarities = new LinkedHashMap<>();
            sortEntriesByScore(similarities, TOP_SIMILARITIES_PER_BOOK)
                    .forEach(entry -> topSimilarities.put(entry.getKey(), entry.getValue()));
            if (!topSimilarities.isEmpty()) {
                redisTemplate.opsForHash().putAll(StrUtil.format(RedisConstants.Cf.SIMILARITY, isbn), topSimilarities);
            }
        });
    }

    private List<String> getCachedRecommendations(String recommendationKey, int limit) {
        List<Object> cachedValues = redisTemplate.opsForList().range(recommendationKey, 0, limit - 1L);
        if (cachedValues == null || cachedValues.isEmpty()) {
            return Collections.emptyList();
        }
        return cachedValues.stream()
                .map(value -> Objects.toString(value, ""))
                .filter(isbn -> !isbn.isBlank())
                .toList();
    }

    private List<String> getUserBorrowedIsbns(Long userId) {
        String userBooksKey = StrUtil.format(RedisConstants.Cf.USER_BOOKS, userId);
        Set<Object> cachedBooks = redisTemplate.opsForSet().members(userBooksKey);
        if (cachedBooks != null && !cachedBooks.isEmpty()) {
            return cachedBooks.stream()
                    .map(value -> Objects.toString(value, ""))
                    .filter(isbn -> !isbn.isBlank())
                    .toList();
        }

        List<String> borrowedIsbns = recommendationMapper.selectUserBorrowedIsbns(userId);
        if (!borrowedIsbns.isEmpty()) {
            redisTemplate.opsForSet().add(userBooksKey, borrowedIsbns.toArray());
            redisTemplate.expire(userBooksKey, USER_BOOKS_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return borrowedIsbns;
    }

    private List<String> sortByScore(Map<String, Double> candidateScores, int limit) {
        return sortEntriesByScore(candidateScores, limit).stream()
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<Map.Entry<String, Double>> sortEntriesByScore(Map<String, Double> scores, int limit) {
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted((left, right) -> {
                    int scoreCompare = Double.compare(right.getValue(), left.getValue());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return left.getKey().compareTo(right.getKey());
                })
                .limit(limit)
                .toList();
    }

    private void cacheRecommendations(String recommendationKey, List<String> recommendations) {
        if (recommendations.isEmpty()) {
            return;
        }
        redisTemplate.delete(recommendationKey);
        redisTemplate.opsForList().rightPushAll(recommendationKey, new ArrayList<>(recommendations));
        redisTemplate.expire(recommendationKey, USER_RECOMMENDATION_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                log.warn("协同过滤相似度分值格式异常：{}", text);
            }
        }
        return 0D;
    }
}
