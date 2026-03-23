package org.dwtech.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.PageResult;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.framework.ai.AISearchService;
import org.springframework.ai.chat.model.ChatResponse;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.service.StockService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
/**
 * IndexController
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Slf4j
@RestController
@RequiredArgsConstructor
public class IndexController extends BaseController {
    private final AISearchService aiSearchService;
    private final StockService stockService;

    /**
     * 用途：执行 index 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    @GetMapping
    public Result<String> index() {
        return Result.success("欢迎使用图书管理系统！");
    }

    /**
     * 用途：分页获取公开书目展示数据。
     *
     * @param queryParams query params
     * @return 返回结果
     */
    @GetMapping("/api/v1/index/books/page")
    public PageResult<PublicBookPageVO> getPublicBookPage(@Valid PublicBookPageQuery queryParams) {
        IPage<PublicBookPageVO> result = stockService.getPublicBookPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 用途：执行 stream chat 操作。
     * 
     * @param message message
     * @return 返回结果
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(@RequestParam(value = "message", defaultValue = "你好", required = false) String message) {
        Long conversationId = SecurityUtils.getUserId();
        return aiSearchService.expandSearchKeywords(message, conversationId);
    }
}
