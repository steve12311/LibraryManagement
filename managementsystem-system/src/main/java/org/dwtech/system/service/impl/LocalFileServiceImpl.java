package org.dwtech.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.system.mapper.FileObjectMapper;
import org.dwtech.system.mapper.FileRecordMapper;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.dwtech.system.model.bo.FileMetaCacheBO;
import org.dwtech.system.model.entity.FileObjectPO;
import org.dwtech.system.model.entity.FileRecordPO;
import org.dwtech.system.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
    private static final String NULL_CACHE_VALUE = "__NULL__";
    private static final long CACHE_TTL_JITTER_MAX_SECONDS = 600L;

    @Value("${oss.local.storage-path}")
    private String storagePath;

    @Value("${file.cache.enabled:true}")
    private boolean fileMetaCacheEnabled = true;

    @Value("${file.cache.meta-ttl-seconds:86400}")
    private long fileMetaCacheTtlSeconds = TimeUnit.DAYS.toSeconds(1);

    @Value("${file.cache.null-ttl-seconds:60}")
    private long fileMetaNullTtlSeconds = 60L;

    private final FileObjectMapper fileObjectMapper;
    private final FileRecordMapper fileRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

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
            FileMetaCacheBO fileMetaCacheBO = toFileMetaCache(fileRecord.getId(), fileRecord, fileObject);
            runAfterCommitOrNow(() -> cacheFileMeta(fileMetaCacheBO));
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

        FileMetaCacheBO fileMetaCacheBO = getFileMetaFromCache(fileId);
        if (fileMetaCacheBO == null && isNullMetaCacheHit(fileId)) {
            throw new RuntimeException("文件不存在");
        }

        if (fileMetaCacheBO == null) {
            fileMetaCacheBO = loadFileMetaFromDb(fileId);
            if (fileMetaCacheBO == null) {
                cacheNullFileMeta(fileId);
                throw new RuntimeException("文件不存在");
            }
            cacheFileMeta(fileMetaCacheBO);
        }
        validateFileAccess(fileMetaCacheBO.getOwnerUserId());

        Path targetPath = resolveSafePath(fileMetaCacheBO.getStoragePath());
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            evictFileMeta(fileId);
            throw new RuntimeException("文件不存在");
        }

        String mimeType = StrUtil.blankToDefault(fileMetaCacheBO.getMimeType(), probeMimeType(targetPath));
        long fileSize = fileMetaCacheBO.getFileSize() == null ? targetPath.toFile().length() : fileMetaCacheBO.getFileSize();

        FileDownloadBO fileDownloadBO = new FileDownloadBO();
        fileDownloadBO.setFilePath(targetPath);
        fileDownloadBO.setFileName(StrUtil.blankToDefault(fileMetaCacheBO.getOriginalName(), fileMetaCacheBO.getSha256()));
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
        validateFileAccess(fileRecord.getOwnerUserId());

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

        Long finalFileId = fileId;
        runAfterCommitOrNow(() -> {
            evictFileMeta(finalFileId);
            cacheNullFileMeta(finalFileId);
        });
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

    private FileMetaCacheBO loadFileMetaFromDb(Long fileId) {
        FileRecordPO fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            return null;
        }
        FileObjectPO fileObject = fileObjectMapper.selectById(fileRecord.getObjectId());
        if (fileObject == null) {
            return null;
        }
        return toFileMetaCache(fileId, fileRecord, fileObject);
    }

    private FileMetaCacheBO toFileMetaCache(Long fileId, FileRecordPO fileRecord, FileObjectPO fileObject) {
        FileMetaCacheBO fileMetaCacheBO = new FileMetaCacheBO();
        fileMetaCacheBO.setFileId(fileId);
        fileMetaCacheBO.setObjectId(fileObject.getId());
        fileMetaCacheBO.setOriginalName(fileRecord.getOriginalName());
        fileMetaCacheBO.setOwnerUserId(fileRecord.getOwnerUserId());
        fileMetaCacheBO.setStoragePath(fileObject.getStoragePath());
        fileMetaCacheBO.setMimeType(fileObject.getMimeType());
        fileMetaCacheBO.setFileSize(fileObject.getFileSize());
        fileMetaCacheBO.setSha256(fileObject.getSha256());
        return fileMetaCacheBO;
    }

    private void cacheFileMeta(FileMetaCacheBO fileMetaCacheBO) {
        if (!fileMetaCacheEnabled || fileMetaCacheBO == null || fileMetaCacheBO.getFileId() == null) {
            return;
        }
        String metaKey = StrUtil.format(RedisConstants.System.FILE_META, fileMetaCacheBO.getFileId());
        String nullKey = StrUtil.format(RedisConstants.System.FILE_META_NULL, fileMetaCacheBO.getFileId());
        long ttlSeconds = Math.max(1L, fileMetaCacheTtlSeconds)
                + ThreadLocalRandom.current().nextLong(CACHE_TTL_JITTER_MAX_SECONDS + 1);

        try {
            ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
            valueOps.set(metaKey, fileMetaCacheBO, ttlSeconds, TimeUnit.SECONDS);
            redisTemplate.delete(nullKey);
        } catch (Exception e) {
            log.warn("写入文件元数据缓存失败，fileId={}", fileMetaCacheBO.getFileId(), e);
        }
    }

    private void cacheNullFileMeta(Long fileId) {
        if (!fileMetaCacheEnabled || fileId == null) {
            return;
        }
        String metaKey = StrUtil.format(RedisConstants.System.FILE_META, fileId);
        String nullKey = StrUtil.format(RedisConstants.System.FILE_META_NULL, fileId);
        long ttlSeconds = Math.max(1L, fileMetaNullTtlSeconds);
        try {
            ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
            valueOps.set(nullKey, NULL_CACHE_VALUE, ttlSeconds, TimeUnit.SECONDS);
            redisTemplate.delete(metaKey);
        } catch (Exception e) {
            log.warn("写入文件空值缓存失败，fileId={}", fileId, e);
        }
    }

    private void evictFileMeta(Long fileId) {
        if (!fileMetaCacheEnabled || fileId == null) {
            return;
        }
        String metaKey = StrUtil.format(RedisConstants.System.FILE_META, fileId);
        String nullKey = StrUtil.format(RedisConstants.System.FILE_META_NULL, fileId);
        try {
            redisTemplate.delete(metaKey);
            redisTemplate.delete(nullKey);
        } catch (Exception e) {
            log.warn("删除文件元数据缓存失败，fileId={}", fileId, e);
        }
    }

    private FileMetaCacheBO getFileMetaFromCache(Long fileId) {
        if (!fileMetaCacheEnabled || fileId == null) {
            return null;
        }
        String metaKey = StrUtil.format(RedisConstants.System.FILE_META, fileId);
        try {
            Object cacheValue = redisTemplate.opsForValue().get(metaKey);
            if (cacheValue instanceof FileMetaCacheBO meta) {
                return meta;
            }
            if (cacheValue instanceof Map<?, ?> mapValue) {
                return mapToFileMeta(mapValue);
            }
            return null;
        } catch (Exception e) {
            log.warn("读取文件元数据缓存失败，fileId={}", fileId, e);
            return null;
        }
    }

    private boolean isNullMetaCacheHit(Long fileId) {
        if (!fileMetaCacheEnabled || fileId == null) {
            return false;
        }
        String nullKey = StrUtil.format(RedisConstants.System.FILE_META_NULL, fileId);
        try {
            Object nullValue = redisTemplate.opsForValue().get(nullKey);
            return nullValue != null && StrUtil.equals(NULL_CACHE_VALUE, String.valueOf(nullValue));
        } catch (Exception e) {
            log.warn("读取文件空值缓存失败，fileId={}", fileId, e);
            return false;
        }
    }

    private FileMetaCacheBO mapToFileMeta(Map<?, ?> mapValue) {
        FileMetaCacheBO fileMetaCacheBO = new FileMetaCacheBO();
        fileMetaCacheBO.setFileId(toLong(mapValue.get("fileId")));
        fileMetaCacheBO.setObjectId(toLong(mapValue.get("objectId")));
        fileMetaCacheBO.setOriginalName(toStringValue(mapValue.get("originalName")));
        fileMetaCacheBO.setOwnerUserId(toLong(mapValue.get("ownerUserId")));
        fileMetaCacheBO.setStoragePath(toStringValue(mapValue.get("storagePath")));
        fileMetaCacheBO.setMimeType(toStringValue(mapValue.get("mimeType")));
        fileMetaCacheBO.setFileSize(toLong(mapValue.get("fileSize")));
        fileMetaCacheBO.setSha256(toStringValue(mapValue.get("sha256")));
        if (StrUtil.isBlank(fileMetaCacheBO.getStoragePath())) {
            return null;
        }
        return fileMetaCacheBO;
    }

    private void validateFileAccess(Long ownerUserId) {
        if (SecurityUtils.isRoot()) {
            return;
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null || ownerUserId == null || !ownerUserId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.ACCESS_UNAUTHORIZED, "无权访问该文件");
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void runAfterCommitOrNow(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }

    private record UploadDigest(Path tempFilePath, String sha256, long fileSize) {
    }
}
