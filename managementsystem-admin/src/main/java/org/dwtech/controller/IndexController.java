package org.dwtech.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.framework.ai.AISearchService;
import org.springframework.ai.chat.model.ChatResponse;
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

    @GetMapping
    public Result<String> index() {
        return Result.success("欢迎使用图书管理系统！");
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(@RequestParam(value = "message", defaultValue = "你好", required = false) String message) {
        Long conversationId = SecurityUtils.getUserId();
        return aiSearchService.expandSearchKeywords(message, conversationId);
    }
}
