package org.dwtech.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.FileObjectMapper;
import org.dwtech.system.mapper.FileRecordMapper;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.dwtech.system.model.entity.FileObjectPO;
import org.dwtech.system.model.entity.FileRecordPO;
import org.dwtech.system.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
/**
 * LocalFileServiceImpl
 *
 * @author steve12311
 * @since 2026-02-22
 */

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "oss.type", havingValue = "local")
public class LocalFileServiceImpl implements FileService {
    private static final int BUFFER_SIZE = 8192;
    private static final String OBJECT_DIR = ".objects";

    @Value("${oss.local.storage-path}")
    private String storagePath;

    private final FileObjectMapper fileObjectMapper;
    private final FileRecordMapper fileRecordMapper;

    /**
     * 用途：执行 upload file 操作。
     *
     * @param file file
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadFile(MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");
        Assert.isFalse(file.isEmpty(), "上传文件不能为空");

        String originalFilename = normalizeFileName(file.getOriginalFilename());
        String suffix = FileUtil.getSuffix(originalFilename);

        UploadDigest uploadDigest;
        try {
            uploadDigest = cacheFileAndDigest(file);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败");
        }

        try {
            FileObjectPO fileObject = findByHash(uploadDigest.sha256(), uploadDigest.fileSize());
            if (fileObject == null) {
                fileObject = createFileObject(uploadDigest, suffix, file.getContentType());
            } else {
                incrementRefCount(fileObject.getId());
            }

            FileRecordPO fileRecord = new FileRecordPO();
            fileRecord.setObjectId(fileObject.getId());
            fileRecord.setOriginalName(originalFilename);
            fileRecord.setOwnerUserId(SecurityUtils.getUserId());
            fileRecord.setBizType("default");
            fileRecord.setIsDeleted(0);
            fileRecordMapper.insert(fileRecord);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setName(originalFilename);
            fileInfo.setUrl("/" + fileRecord.getId());
            return fileInfo;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败");
        } finally {
            // 命中去重时仍会生成临时文件，这里统一清理。
            FileUtil.del(uploadDigest.tempFilePath().toFile());
        }
    }

    /**
     * 用途：根据 fileId 获取文件信息。
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    @Override
    public FileDownloadBO getFile(Long fileId) {
        Assert.notNull(fileId, "文件ID不能为空");
        FileRecordPO fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new RuntimeException("文件不存在");
        }

        FileObjectPO fileObject = fileObjectMapper.selectById(fileRecord.getObjectId());
        if (fileObject == null) {
            throw new RuntimeException("文件不存在");
        }

        Path targetPath = resolveSafePath(fileObject.getStoragePath());
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            throw new RuntimeException("文件不存在");
        }

        String mimeType = StrUtil.blankToDefault(fileObject.getMimeType(), probeMimeType(targetPath));
        long fileSize = fileObject.getFileSize() == null ? targetPath.toFile().length() : fileObject.getFileSize();

        FileDownloadBO fileDownloadBO = new FileDownloadBO();
        fileDownloadBO.setFilePath(targetPath);
        fileDownloadBO.setFileName(StrUtil.blankToDefault(fileRecord.getOriginalName(), fileObject.getSha256()));
        fileDownloadBO.setMimeType(StrUtil.blankToDefault(mimeType, MediaType.APPLICATION_OCTET_STREAM_VALUE));
        fileDownloadBO.setFileSize(fileSize);
        return fileDownloadBO;
    }

    /**
     * 用途：删除 file。
     *
     * @param fileId 文件ID
     * @return 操作结果，true 表示成功，false 表示失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long fileId) {
        if (fileId == null) {
            return false;
        }
        FileRecordPO fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            return false;
        }

        int removed = fileRecordMapper.deleteById(fileId);
        if (removed == 0) {
            return false;
        }

        Long objectId = fileRecord.getObjectId();
        if (objectId == null) {
            return true;
        }

        fileObjectMapper.update(
                null,
                new LambdaUpdateWrapper<FileObjectPO>()
                        .eq(FileObjectPO::getId, objectId)
                        .gt(FileObjectPO::getRefCount, 0)
                        .setSql("ref_count = ref_count - 1")
        );
        FileObjectPO fileObject = fileObjectMapper.selectById(objectId);
        if (fileObject != null && fileObject.getRefCount() != null && fileObject.getRefCount() <= 0) {
            fileObjectMapper.deleteById(objectId);
            Path objectPath = resolveSafePath(fileObject.getStoragePath());
            FileUtil.del(objectPath.toFile());
        }
        return true;
    }

    /**
     * 解析并校验存储路径，防止目录穿越删除/写入。
     *
     * @param relativePath 相对存储根目录的路径
     * @return 安全、规范化后的绝对路径
     */
    private Path resolveSafePath(String relativePath) {
        Path storageRoot = getStorageRoot();
        Path targetPath = storageRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new RuntimeException("非法文件路径");
        }
        return targetPath;
    }

    private Path getStorageRoot() {
        return Path.of(storagePath).toAbsolutePath().normalize();
    }

    private String normalizeFileName(String originalFilename) {
        return StrUtil.blankToDefault(originalFilename, "file");
    }

    private UploadDigest cacheFileAndDigest(MultipartFile file) throws Exception {
        Path storageRoot = getStorageRoot();
        Files.createDirectories(storageRoot);
        Path tempFilePath = Files.createTempFile(storageRoot, "upload-", ".tmp");

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        long fileSize = 0L;
        try (InputStream inputStream = file.getInputStream();
             DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);
             OutputStream outputStream = Files.newOutputStream(tempFilePath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int readLen;
            while ((readLen = digestInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readLen);
                fileSize += readLen;
            }
        }
        String sha256 = HexFormat.of().formatHex(messageDigest.digest());
        return new UploadDigest(tempFilePath, sha256, fileSize);
    }

    private FileObjectPO findByHash(String sha256, long fileSize) {
        return fileObjectMapper.selectOne(
                new LambdaQueryWrapper<FileObjectPO>()
                        .eq(FileObjectPO::getSha256, sha256)
                        .eq(FileObjectPO::getFileSize, fileSize)
                        .last("limit 1")
        );
    }

    private FileObjectPO createFileObject(UploadDigest uploadDigest, String suffix, String mimeType) throws Exception {
        String relativePath = buildObjectPath(uploadDigest.sha256());
        Path targetPath = resolveSafePath(relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.move(uploadDigest.tempFilePath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        FileObjectPO fileObject = new FileObjectPO();
        fileObject.setSha256(uploadDigest.sha256());
        fileObject.setFileSize(uploadDigest.fileSize());
        fileObject.setStoragePath(relativePath);
        fileObject.setMimeType(StrUtil.blankToDefault(mimeType, MediaType.APPLICATION_OCTET_STREAM_VALUE));
        fileObject.setExt(suffix);
        fileObject.setRefCount(1);
        fileObject.setIsDeleted(0);
        try {
            fileObjectMapper.insert(fileObject);
            return fileObject;
        } catch (DuplicateKeyException e) {
            // 并发上传同一文件时，走唯一索引兜底。
            FileObjectPO existed = findByHash(uploadDigest.sha256(), uploadDigest.fileSize());
            if (existed == null) {
                throw e;
            }
            incrementRefCount(existed.getId());
            return existed;
        }
    }

    private void incrementRefCount(Long objectId) {
        fileObjectMapper.update(
                null,
                new LambdaUpdateWrapper<FileObjectPO>()
                        .eq(FileObjectPO::getId, objectId)
                        .setSql("ref_count = ref_count + 1")
        );
    }

    private String probeMimeType(Path targetPath) {
        try {
            return Files.probeContentType(targetPath);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildObjectPath(String sha256) {
        return OBJECT_DIR + "/" + sha256.substring(0, 2) + "/" + sha256.substring(2, 4) + "/" + sha256;
    }

    private record UploadDigest(Path tempFilePath, String sha256, long fileSize) {
    }
}
