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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LocalFileServiceImpl
 * 本地文件存储服务实现。通过 oss.type=local 条件激活。
 * 支持文件上传（含 SHA-256 去重）、下载信息获取和删除操作，
 * 使用 Redis 缓存文件元数据以提升访问性能。
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

    @Value("${file.upload.max-size-bytes:5242880}")
    private long maxUploadSizeBytes = 5L * 1024 * 1024;

    @Value("${file.upload.allowed-extensions:jpg,jpeg,png,gif}")
    private String allowedUploadExtensions = "jpg,jpeg,png,gif";

    @Value("${file.upload.allowed-mime-types:image/jpeg,image/png,image/gif}")
    private String allowedUploadMimeTypes = "image/jpeg,image/png,image/gif";

    @Value("${file.download.inline-allowed-mime-types:image/jpeg,image/png,image/gif}")
    private String inlineAllowedMimeTypes = "image/jpeg,image/png,image/gif";

    private final FileObjectMapper fileObjectMapper;
    private final FileRecordMapper fileRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformTransactionManager transactionManager;

    /**
     * 上传文件。流程：校验文件类型和大小 → 缓存临时文件并计算 SHA-256 → 按指纹去重 →
     * 创建或复用文件对象 → 写入文件记录 → 缓存元数据 → 返回文件访问信息。
     *
     * @param file 待上传的文件
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadFile(MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");
        Assert.isFalse(file.isEmpty(), "上传文件不能为空");

        String originalFilename = normalizeFileName(file.getOriginalFilename());
        String suffix = StrUtil.blankToDefault(FileUtil.getSuffix(originalFilename), "").toLowerCase(Locale.ROOT);
        validateUploadRequest(file, suffix);

        UploadDigest uploadDigest;
        try {
            uploadDigest = cacheFileAndDigest(file);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败");
        }

        try {
            String mimeType = validateAndResolveUploadMimeType(uploadDigest.tempFilePath(), suffix);
            FileObjectPO fileObject = findByHash(uploadDigest.sha256(), uploadDigest.fileSize());
            if (fileObject == null) {
                fileObject = createFileObject(uploadDigest, suffix, mimeType);
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
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败");
        } finally {
            FileUtil.del(uploadDigest.tempFilePath().toFile());
        }
    }

    /**
     * 根据文件 ID 获取文件下载信息。流程：读取缓存元数据 → 若缓存空值可命中则快速失败 →
     * 未命中则从数据库加载并写入缓存 → 校验文件存在性 → 构造下载信息返回。
     *
     * @param fileId 文件 ID
     * @return 文件下载信息
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
        fileDownloadBO.setInlineAllowed(isInlineMimeType(mimeType));
        return fileDownloadBO;
    }

    /**
     * 物理删除文件记录（需调用方校验 sys:file:del 权限）。
     * 无视引用计数，直接删除文件记录、文件对象和物理文件。
     *
     * @param fileId 文件 ID
     * @return true 表示删除成功，false 表示文件不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFilePhysical(Long fileId) {
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
        if (objectId != null) {
            FileObjectPO fileObject = fileObjectMapper.selectById(objectId);
            if (fileObject != null) {
                fileObjectMapper.deleteById(objectId);
                Path objectPath = resolveSafePath(fileObject.getStoragePath());
                FileUtil.del(objectPath.toFile());
            }
        }

        runAfterCommitOrNow(() -> {
            evictFileMeta(fileId);
            cacheNullFileMeta(fileId);
        });
        return true;
    }

    /**
     * 引用计数删除文件记录。删除文件记录，文件对象引用计数减一，
     * 仅当引用归零时才删除物理文件。供消息队列异步调用。
     *
     * @param fileId 文件 ID
     * @return true 表示删除成功，false 表示文件不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFileByRefCount(Long fileId) {
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
            runAfterCommitOrNow(() -> {
                evictFileMeta(fileId);
                cacheNullFileMeta(fileId);
            });
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

        runAfterCommitOrNow(() -> {
            evictFileMeta(fileId);
            cacheNullFileMeta(fileId);
        });
        return true;
    }

    /**
     * 获取文件对象的引用计数。
     *
     * @param fileId 文件 ID
     * @return 引用计数，文件不存在时返回 0
     */
    @Override
    public int getFileRefCount(Long fileId) {
        if (fileId == null) {
            return 0;
        }
        FileRecordPO fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null || fileRecord.getObjectId() == null) {
            return 0;
        }
        FileObjectPO fileObject = fileObjectMapper.selectById(fileRecord.getObjectId());
        return (fileObject != null && fileObject.getRefCount() != null) ? fileObject.getRefCount() : 0;
    }

    /**
     * 解析并规范化存储路径，防止目录穿越攻击。
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

    private void validateUploadRequest(MultipartFile file, String suffix) {
        if (file.getSize() > maxUploadSizeBytes) {
            throw new BusinessException(ResultCode.UPLOAD_IMAGE_TOO_LARGE);
        }
        if (StrUtil.isBlank(suffix) || !parseConfigSet(allowedUploadExtensions).contains(suffix)) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_TYPE_MISMATCH, "仅支持 jpg、jpeg、png、gif 图片上传");
        }
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

    private String validateAndResolveUploadMimeType(Path tempFilePath, String suffix) {
        if (!isSupportedImagePayload(tempFilePath)) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_TYPE_MISMATCH, "上传内容不是有效的图片文件");
        }

        String detectedMimeType = normalizeMimeType(detectMimeType(tempFilePath));
        String expectedMimeType = expectedMimeType(suffix);
        if (StrUtil.isBlank(detectedMimeType)) {
            detectedMimeType = expectedMimeType;
        }
        if (StrUtil.isBlank(detectedMimeType) || !parseConfigSet(allowedUploadMimeTypes).contains(detectedMimeType)) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_TYPE_MISMATCH, "仅支持 jpg、jpeg、png、gif 图片上传");
        }
        if (StrUtil.isNotBlank(expectedMimeType) && !StrUtil.equals(expectedMimeType, detectedMimeType)) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_TYPE_MISMATCH, "文件后缀与真实内容类型不匹配");
        }
        return detectedMimeType;
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
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            FileObjectPO existed = txTemplate.execute(status -> {
                FileObjectPO found = findByHash(uploadDigest.sha256(), uploadDigest.fileSize());
                if (found != null) {
                    return found;
                }
                return fileObjectMapper.selectByHashIgnoreDeleted(uploadDigest.sha256(), uploadDigest.fileSize());
            });
            if (existed == null) {
                throw e;
            }
            if (existed.getIsDeleted() != null && existed.getIsDeleted() == 1) {
                fileObjectMapper.reactivateFileObject(existed.getId(), relativePath);
                existed.setIsDeleted(0);
                existed.setRefCount(1);
            } else {
                incrementRefCount(existed.getId());
            }
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

    private String detectMimeType(Path targetPath) {
        try (InputStream inputStream = Files.newInputStream(targetPath)) {
            return URLConnection.guessContentTypeFromStream(inputStream);
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

    void evictFileMeta(Long fileId) {
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

    private boolean isInlineMimeType(String mimeType) {
        return parseConfigSet(inlineAllowedMimeTypes).contains(normalizeMimeType(mimeType));
    }

    private boolean isSupportedImagePayload(Path targetPath) {
        try (InputStream inputStream = Files.newInputStream(targetPath)) {
            return ImageIO.read(inputStream) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String expectedMimeType(String suffix) {
        return switch (StrUtil.blankToDefault(suffix, "").toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            default -> null;
        };
    }

    private Set<String> parseConfigSet(String configValue) {
        return Arrays.stream(StrUtil.blankToDefault(configValue, "").split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private String normalizeMimeType(String mimeType) {
        if (StrUtil.isBlank(mimeType)) {
            return null;
        }
        return mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
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

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
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
