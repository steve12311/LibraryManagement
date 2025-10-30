package org.dwtech.controller;

import org.dwtech.common.core.controller.BaseController;
import org.dwtech.common.core.entity.AjaxResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController extends BaseController {

    @GetMapping
    @PreAuthorize("@yz.hasPermit('system:demo:test')")
    public AjaxResult test() {
        return AjaxResult.success();
    }
}
