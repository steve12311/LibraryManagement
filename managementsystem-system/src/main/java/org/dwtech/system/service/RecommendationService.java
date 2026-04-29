package org.dwtech.system.service;

import java.util.List;

/**
 * 图书协同过滤推荐服务
 *
 * @author steve12311
 * @since 2026-04-29
 */
public interface RecommendationService {
    /**
     * 获取用户 Top-N 推荐 ISBN。
     *
     * @param userId 用户 ID
     * @param limit 推荐数量上限
     * @return 推荐 ISBN 列表
     */
    List<String> getRecommendedIsbns(Long userId, int limit);

    /**
     * 清除用户推荐相关缓存。
     *
     * @param userId 用户 ID
     */
    void invalidateUserCache(Long userId);

    /**
     * 重建物品相似度矩阵。
     */
    void rebuildSimilarityMatrix();
}
