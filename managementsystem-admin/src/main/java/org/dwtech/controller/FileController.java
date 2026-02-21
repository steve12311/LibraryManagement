package org.dwtech.controller;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileService fileService;

    @PostMapping
    @RepeatSubmit
    public Result<FileInfo> uploadFile(@RequestParam("file") MultipartFile file) {
        FileInfo fileInfo = fileService.uploadFile(file);
        return Result.success(fileInfo);
    }
}
