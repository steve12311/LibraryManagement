package org.dwtech.controller;

import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class IndexController extends BaseController {

    @GetMapping
    public AjaxResult index() {
        return AjaxResult.success("欢迎使用图书管理系统！");
    }
}
