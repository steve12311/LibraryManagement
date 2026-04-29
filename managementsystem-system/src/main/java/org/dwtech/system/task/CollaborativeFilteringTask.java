package org.dwtech.system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.system.service.RecommendationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 协同过滤推荐调度任务
 *
 * @author steve12311
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborativeFilteringTask {
    private final RecommendationService recommendationService;

    /**
     * 每天凌晨 3 点重建图书相似度矩阵。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void rebuildSimilarityMatrix() {
        recommendationService.rebuildSimilarityMatrix();
        log.info("协同过滤图书相似度矩阵重建任务执行完成");
    }
}
