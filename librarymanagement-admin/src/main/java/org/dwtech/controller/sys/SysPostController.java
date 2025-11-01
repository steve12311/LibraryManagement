package org.dwtech.controller.sys;

import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.core.entity.dto.SysPostDto;
import org.dwtech.common.exception.NotValidException;
import org.dwtech.common.valid.SysAddPostGroup;
import org.dwtech.common.valid.SysEditPostGroup;
import org.dwtech.system.service.SysPostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/post")
public class SysPostController {
    private final SysPostService sysPostService;

    public SysPostController(SysPostService sysPostService) {
        this.sysPostService = sysPostService;
    }

    @GetMapping("/list")
    @PreAuthorize(value = "@yz.hasPermit('system:post:list')")
    public AjaxResult getPosts(SysPostDto sysPostDto) {
        return AjaxResult.success(sysPostService.selectPostList(sysPostDto));
    }

    @PostMapping
    @PreAuthorize(value = "@yz.hasPermit('system:post:add')")
    public AjaxResult addPost(@Validated(SysAddPostGroup.class) @RequestBody SysPostDto sysPostDto) {
        if (sysPostService.checkPostNameUnique(sysPostDto)) {
            throw new NotValidException(String.format("岗位：%s 已存在", sysPostDto.getPostName()));
        }
        return AjaxResult.success(sysPostService.insertPost(sysPostDto));
    }

    @PutMapping
    @PreAuthorize(value = "@yz.hasPermit('system:post:edit')")
    public AjaxResult editPost(@Validated(SysEditPostGroup.class) @RequestBody SysPostDto sysPostDto) {
        return AjaxResult.success(sysPostService.updatePost(sysPostDto));
    }

    @DeleteMapping("/{postIds}")
    @PreAuthorize(value = "@yz.hasPermit('system:post:del')")
    public AjaxResult deletePosts(@PathVariable(value = "postIds") Long[] postIds) {
        return AjaxResult.success(sysPostService.deletePost(postIds));
    }
}
