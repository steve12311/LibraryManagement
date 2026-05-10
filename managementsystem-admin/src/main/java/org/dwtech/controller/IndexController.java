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
import org.dwtech.system.model.vo.PublicLibraryFloorDetailVO;
import org.dwtech.system.model.vo.PublicLibraryFloorVO;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.service.LibraryMapService;
import org.dwtech.system.service.StockService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
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
    private final LibraryMapService libraryMapService;

    /**
     * 首页接口，返回系统欢迎信息，用于健康检查和访问确认。
     */
    @GetMapping
    public Result<String> index() {
        return Result.success("欢迎使用图书管理系统！");
    }

    /**
     * 分页获取公开书目展示数据。
     * 首页展示用，仅返回已上架且可借阅的图书，包含封面、书名、作者等摘要信息。
     */
    @GetMapping("/api/v1/index/books/page")
    public PageResult<PublicBookPageVO> getPublicBookPage(@Valid PublicBookPageQuery queryParams) {
        IPage<PublicBookPageVO> result = stockService.getPublicBookPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 查询公开楼层列表，用于首页书架地图楼层切换。
     */
    @GetMapping("/api/v1/index/library-map/floors")
    public Result<List<PublicLibraryFloorVO>> listPublicFloors() {
        return Result.success(libraryMapService.listPublicFloors());
    }

    /**
     * 查询公开楼层地图详情。
     * 返回楼层轮廓、当前层启用书架及每个书架的图书摘要。
     */
    @GetMapping("/api/v1/index/library-map/floors/{floorId}")
    public Result<PublicLibraryFloorDetailVO> getPublicFloorDetail(@PathVariable("floorId") Long floorId) {
        return Result.success(libraryMapService.getPublicFloorDetail(floorId));
    }

    /**
     * AI 智能搜索对话接口。
     * 接收用户自然语言消息，以 SSE 流式返回搜索结果，用于首页 AI 搜索助手交互。
     *
     * @param message 用户输入的自然语言搜索消息
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(@RequestParam(value = "message", defaultValue = "你好", required = false) String message) {
        Long conversationId = SecurityUtils.getUserId();
        return aiSearchService.expandSearchKeywords(message, conversationId);
    }
}
