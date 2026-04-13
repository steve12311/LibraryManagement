package org.dwtech.framework.ai.vector.rebuild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CatalogVectorRebuildRunner
 *
 * @author steve12311
 * @since 2026-04-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.catalog-vector-rebuild", name = "enabled", havingValue = "true")
public class CatalogVectorRebuildRunner implements ApplicationRunner {
    private final CatalogVectorRebuildService catalogVectorRebuildService;
    private final CatalogVectorRebuildProperties rebuildProperties;

    /**
     * 用途：启动时按配置执行一次馆藏向量重建。
     *
     * @param args 应用启动参数
     * 返回：无。
     */
    @Override
    public void run(ApplicationArguments args) {
        CatalogVectorRebuildService.CatalogVectorRebuildSummary summary =
                catalogVectorRebuildService.rebuildCatalogVectors(rebuildProperties.getBatchSize());
        log.info(
                "action=rebuild_catalog_vector result=completed scanned={} synced={} skipped={} failed={}",
                summary.scanned(),
                summary.synced(),
                summary.skipped(),
                summary.failed()
        );
    }
}
