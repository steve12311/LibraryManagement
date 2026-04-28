package org.dwtech.controller;

import lombok.RequiredArgsConstructor;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.common.core.entity.Result;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.dwtech.system.service.FileService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
/**
 * FileController
 *
 * @author steve12311
 * @since 2026-02-22
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileService fileService;

    /**
     * 上传文件，支持图片、文档等附件类型。
     * 文件由服务端存储并返回 FileInfo 对象，包含访问路径和元数据。
     *
     * @param file 上传的 Multipart 文件
     */
    @PostMapping
    @RepeatSubmit
    @PreAuthorize("isAuthenticated()")
    @OperLog(module = "文件管理", action = "上传文件", bizId = "#p0.originalFilename")
    public Result<FileInfo> uploadFile(@RequestParam("file") MultipartFile file) {
        FileInfo fileInfo = fileService.uploadFile(file);
        return Result.success(fileInfo);
    }

    /**
     * 根据文件 ID 获取文件内容，支持浏览器内联预览或附件下载。
     * 根据 MIME 类型和可预览标识自动决定 Content-Disposition 为 inline 或 attachment。
     *
     * @param fileId 文件记录 ID
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<UrlResource> getFile(@PathVariable("fileId") Long fileId) throws Exception {
        FileDownloadBO fileDownloadBO = fileService.getFile(fileId);
        UrlResource resource = new UrlResource(fileDownloadBO.getFilePath().toUri());

        MediaType mediaType;
        ContentDisposition contentDisposition;
        if (fileDownloadBO.isInlineAllowed()) {
            try {
                mediaType = MediaType.parseMediaType(fileDownloadBO.getMimeType());
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            contentDisposition = ContentDisposition.inline()
                    .filename(fileDownloadBO.getFileName(), StandardCharsets.UTF_8)
                    .build();
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
            contentDisposition = ContentDisposition.attachment()
                    .filename(fileDownloadBO.getFileName(), StandardCharsets.UTF_8)
                    .build();
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileDownloadBO.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    /**
     * 删除指定文件记录。只能删除当前用户拥有的文件。
     *
     * @param fileId 文件记录 ID
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @OperLog(module = "文件管理", action = "删除文件", bizId = "#p0")
    public Result<Boolean> deleteFile(@PathVariable("fileId") Long fileId) {
        boolean deleted = fileService.deleteFile(fileId);
        return Result.success(deleted);
    }
}
