package org.dwtech.system.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.system.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
/**
 * LocalFileServiceImpl
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Component
@Slf4j
@ConditionalOnProperty(value = "oss.type", havingValue = "local")
@ConfigurationProperties(prefix = "oss.local")
public class LocalFileServiceImpl implements FileService {
    @Value("${oss.local.storage-path}")
    private String storagePath;

    /**
     * 用途：执行 upload file 操作。
     * 
     * @param file file
     * @return 返回结果
     */
    @Override
    public FileInfo uploadFile(MultipartFile file) {
        // 获取文件名
        String originalFilename = file.getOriginalFilename();
        // 获取文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 生成uuid
        String fileName = StrUtil.isBlank(suffix) ? IdUtil.simpleUUID() : IdUtil.simpleUUID() + "." + suffix;
        // 生成文件名(日期文件夹)
        String folder = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        Path targetPath = resolveSafePath(folder + File.separator + fileName);
        //  try-with-resource 语法糖自动释放流
        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(targetPath.getParent());
            // 上传文件
            FileUtil.writeFromStream(inputStream, targetPath.toString());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败");
        }
        // 获取文件访问路径，因为这里是本地存储，所以直接返回文件的相对路径，需要前端自行处理访问前缀
        String fileUrl = "/" + folder + "/" + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    /**
     * 用途：删除 file。
     * 
     * @param filePath file path
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    public boolean deleteFile(String filePath) {
        //判断文件是否为空
        if (StrUtil.isBlank(filePath)) {
            return false;
        }
        String normalizedPath = filePath.replace("\\", "/");
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (StrUtil.isBlank(normalizedPath)) {
            return false;
        }
        Path targetPath;
        try {
            targetPath = resolveSafePath(normalizedPath);
        } catch (RuntimeException ex) {
            log.warn("非法文件路径: {}", filePath);
            return false;
        }
        // 判断filepath是否为文件夹
        if (Files.isDirectory(targetPath)) {
            // 禁止删除文件夹
            return false;
        }
        // 删除文件
        return FileUtil.del(targetPath.toFile());
    }

    /**
     * 解析并校验存储路径，防止目录穿越删除/写入。
     *
     * @param relativePath 相对存储根目录的路径
     * @return 安全、规范化后的绝对路径
     */
    private Path resolveSafePath(String relativePath) {
        Path storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        Path targetPath = storageRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new RuntimeException("非法文件路径");
        }
        return targetPath;
    }
}
