package org.dwtech.framework.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.framework.ai.vector.store.CatalogVectorStoreService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
/**
 * VectorTool
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorTool {
    private final CatalogVectorStoreService catalogVectorStoreService;

    /**
     * 根据关键词在向量库中搜索图书，返回匹配的 ISBN 编码集合，供 AI 模型使用。
     *
     * @param keywords 搜索关键词列表
     * @return 匹配的 ISBN 编码集合
     */
    @Tool(description = "通过关键词搜索书库，返回相关书籍对应的ISBN码")
    public Set<String> searchVectors(List<String> keywords) {
        log.info("关键词{}", keywords.toString());
        Set<String> isbns = catalogVectorStoreService.searchCatalogBookIsbns(keywords);
        log.info("命中ISBN集合{}", isbns);
        return isbns;
    }
}
