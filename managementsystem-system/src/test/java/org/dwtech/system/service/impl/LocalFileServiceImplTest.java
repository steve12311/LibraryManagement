package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.hutool.core.util.StrUtil;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.common.service.PermissionService;
import org.dwtech.system.mapper.BookMapper;
import org.dwtech.system.mapper.FileObjectMapper;
import org.dwtech.system.mapper.FileRecordMapper;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.dwtech.system.model.bo.FileMetaCacheBO;
import org.dwtech.system.model.entity.FileObjectPO;
import org.dwtech.system.model.entity.FileRecordPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFileServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileObjectMapper fileObjectMapper;

    @Mock
    private FileRecordMapper fileRecordMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private LocalFileServiceImpl localFileService;

    @BeforeEach
    void setUp() {
        localFileService = new LocalFileServiceImpl(fileObjectMapper, fileRecordMapper, bookMapper, permissionService, redisTemplate);
        ReflectionTestUtils.setField(localFileService, "storagePath", tempDir.toString());
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "root",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ROOT"))
                )
        );
    }

    @Test
    void shouldUploadFileAndReturnFileIdUrlWhenObjectNotExists() throws Exception {
        byte[] content = pngBytes();
        String expectedSha256 = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                content
        );

        when(fileObjectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            FileObjectPO fileObjectPO = invocation.getArgument(0);
            fileObjectPO.setId(11L);
            return 1;
        }).when(fileObjectMapper).insert(any(FileObjectPO.class));
        doAnswer(invocation -> {
            FileRecordPO fileRecordPO = invocation.getArgument(0);
            fileRecordPO.setId(101L);
            return 1;
        }).when(fileRecordMapper).insert(any(FileRecordPO.class));

        FileInfo fileInfo = localFileService.uploadFile(multipartFile);

        assertThat(fileInfo.getName()).isEqualTo("cover.png");
        assertThat(fileInfo.getUrl()).isEqualTo("/101");
        String metaKey = "system:file:meta:101";
        String nullKey = "system:file:meta:null:101";
        verify(valueOperations).set(eq(metaKey), any(FileMetaCacheBO.class), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete(nullKey);

        ArgumentCaptor<FileObjectPO> objectCaptor = ArgumentCaptor.forClass(FileObjectPO.class);
        verify(fileObjectMapper).insert(objectCaptor.capture());
        FileObjectPO insertedObject = objectCaptor.getValue();
        assertThat(insertedObject.getSha256()).isEqualTo(expectedSha256);
        assertThat(insertedObject.getRefCount()).isEqualTo(1);
        assertThat(insertedObject.getMimeType()).isEqualTo("image/png");
        assertThat(insertedObject.getStoragePath()).startsWith(".objects/");
        Path storedFile = tempDir.resolve(insertedObject.getStoragePath());
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(content);

        ArgumentCaptor<FileRecordPO> recordCaptor = ArgumentCaptor.forClass(FileRecordPO.class);
        verify(fileRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getObjectId()).isEqualTo(11L);
        assertThat(recordCaptor.getValue().getOriginalName()).isEqualTo("cover.png");
    }

    @Test
    void shouldReuseObjectWhenUploadDuplicateFile() {
        byte[] content = pngBytes();
        MockMultipartFile multipartFile1 = new MockMultipartFile(
                "file",
                "a.png",
                "image/png",
                content
        );
        MockMultipartFile multipartFile2 = new MockMultipartFile(
                "file",
                "b.png",
                "image/png",
                content
        );

        FileObjectPO existedObject = new FileObjectPO();
        existedObject.setId(21L);
        when(fileObjectMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null)
                .thenReturn(existedObject);

        doAnswer(invocation -> {
            FileObjectPO fileObjectPO = invocation.getArgument(0);
            fileObjectPO.setId(21L);
            return 1;
        }).when(fileObjectMapper).insert(any(FileObjectPO.class));

        AtomicLong recordId = new AtomicLong(300L);
        doAnswer(invocation -> {
            FileRecordPO fileRecordPO = invocation.getArgument(0);
            fileRecordPO.setId(recordId.getAndIncrement());
            return 1;
        }).when(fileRecordMapper).insert(any(FileRecordPO.class));

        FileInfo first = localFileService.uploadFile(multipartFile1);
        FileInfo second = localFileService.uploadFile(multipartFile2);

        assertThat(first.getUrl()).isEqualTo("/300");
        assertThat(second.getUrl()).isEqualTo("/301");
        verify(fileObjectMapper, times(1)).insert(any(FileObjectPO.class));
        verify(fileObjectMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldRejectUploadWhenFileTypeIsNotAllowed() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "cover.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> localFileService.uploadFile(multipartFile))
                .isInstanceOf(org.dwtech.common.exception.BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(org.dwtech.common.enmus.ResultCode.UPLOAD_FILE_TYPE_MISMATCH);
    }

    @Test
    void shouldRejectUploadWhenFileSizeExceedsLimit() {
        ReflectionTestUtils.setField(localFileService, "maxUploadSizeBytes", 8L);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                pngBytes()
        );

        assertThatThrownBy(() -> localFileService.uploadFile(multipartFile))
                .isInstanceOf(org.dwtech.common.exception.BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(org.dwtech.common.enmus.ResultCode.UPLOAD_IMAGE_TOO_LARGE);
    }

    @Test
    void shouldRejectUploadWhenImagePayloadIsInvalid() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                "not-an-image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> localFileService.uploadFile(multipartFile))
                .isInstanceOf(org.dwtech.common.exception.BusinessException.class)
                .hasMessage("上传内容不是有效的图片文件");
    }

    @Test
    void shouldGetFileByFileId() throws Exception {
        Path objectPath = tempDir.resolve(".objects/aa/bb/abcdef");
        Files.createDirectories(objectPath.getParent());
        Files.writeString(objectPath, "img-content");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(9L);
        fileRecordPO.setObjectId(3L);
        fileRecordPO.setOriginalName("cover.png");
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(9L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(3L);
        fileObjectPO.setStoragePath(".objects/aa/bb/abcdef");
        fileObjectPO.setMimeType("image/png");
        fileObjectPO.setFileSize(11L);
        fileObjectPO.setSha256("abcdef");
        when(fileObjectMapper.selectById(3L)).thenReturn(fileObjectPO);

        FileDownloadBO fileDownloadBO = localFileService.getFile(9L);

        assertThat(fileDownloadBO.getFilePath()).isEqualTo(objectPath.toAbsolutePath().normalize());
        assertThat(fileDownloadBO.getFileName()).isEqualTo("cover.png");
        assertThat(fileDownloadBO.getMimeType()).isEqualTo("image/png");
        assertThat(fileDownloadBO.getFileSize()).isEqualTo(11L);
        assertThat(fileDownloadBO.isInlineAllowed()).isTrue();
    }

    @Test
    void shouldCachePublicBookCoverFlagWhenFileBoundToBookCover() {
        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(9L);
        fileRecordPO.setObjectId(3L);
        fileRecordPO.setOriginalName("cover.png");
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(9L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(3L);
        fileObjectPO.setStoragePath(".objects/aa/bb/abcdef");
        fileObjectPO.setMimeType("image/png");
        fileObjectPO.setFileSize(11L);
        fileObjectPO.setSha256("abcdef");
        when(fileObjectMapper.selectById(3L)).thenReturn(fileObjectPO);
        when(bookMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> localFileService.getFile(9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("文件不存在");

        ArgumentCaptor<FileMetaCacheBO> cacheCaptor = ArgumentCaptor.forClass(FileMetaCacheBO.class);
        verify(valueOperations).set(eq("system:file:meta:9"), cacheCaptor.capture(), anyLong(), eq(TimeUnit.SECONDS));
        assertThat(cacheCaptor.getValue().getPublicBookCover()).isTrue();
    }

    @Test
    void shouldReadFileMetaFromCacheWithoutDbQuery() throws Exception {
        Path objectPath = tempDir.resolve(".objects/aa/bb/from-cache");
        Files.createDirectories(objectPath.getParent());
        Files.writeString(objectPath, "cache-content");

        FileMetaCacheBO cacheBO = new FileMetaCacheBO();
        cacheBO.setFileId(20L);
        cacheBO.setOriginalName("avatar.jpg");
        cacheBO.setOwnerUserId(1001L);
        cacheBO.setStoragePath(".objects/aa/bb/from-cache");
        cacheBO.setMimeType("image/jpeg");
        cacheBO.setFileSize(13L);
        cacheBO.setSha256("cache-sha");

        when(valueOperations.get(StrUtil.format(RedisConstants.System.FILE_META, 20L))).thenReturn(cacheBO);

        FileDownloadBO fileDownloadBO = localFileService.getFile(20L);

        assertThat(fileDownloadBO.getFileName()).isEqualTo("avatar.jpg");
        assertThat(fileDownloadBO.isInlineAllowed()).isTrue();
        verify(fileRecordMapper, never()).selectById(any());
        verify(fileObjectMapper, never()).selectById(any());
    }

    @Test
    void shouldWriteNullCacheWhenFileMissing() {
        Long fileId = 404L;
        String metaKey = StrUtil.format(RedisConstants.System.FILE_META, fileId);
        String nullKey = StrUtil.format(RedisConstants.System.FILE_META_NULL, fileId);
        when(valueOperations.get(metaKey)).thenReturn(null);
        when(valueOperations.get(nullKey)).thenReturn(null);
        when(fileRecordMapper.selectById(fileId)).thenReturn(null);

        assertThatThrownBy(() -> localFileService.getFile(fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("文件不存在");

        verify(valueOperations).set(eq(nullKey), eq("__NULL__"), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete(metaKey);
    }

    @Test
    void shouldDeleteFileAndPhysicalObjectWhenRefCountReachZero() throws Exception {
        Path objectPath = tempDir.resolve(".objects/aa/bb/to-delete");
        Files.createDirectories(objectPath.getParent());
        Files.writeString(objectPath, "will-delete");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(7L);
        fileRecordPO.setObjectId(88L);
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(7L)).thenReturn(fileRecordPO);
        when(fileRecordMapper.deleteById(7L)).thenReturn(1);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(88L);
        fileObjectPO.setStoragePath(".objects/aa/bb/to-delete");
        fileObjectPO.setRefCount(0);
        when(fileObjectMapper.selectById(88L)).thenReturn(fileObjectPO);

        boolean deleted = localFileService.deleteFile(7L);

        assertThat(deleted).isTrue();
        verify(fileObjectMapper).deleteById(88L);
        assertThat(Files.exists(objectPath)).isFalse();
        String metaKey = StrUtil.format(RedisConstants.System.FILE_META, 7L);
        String nullKey = StrUtil.format(RedisConstants.System.FILE_META_NULL, 7L);
        verify(redisTemplate, atLeastOnce()).delete(metaKey);
        verify(valueOperations).set(eq(nullKey), eq("__NULL__"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldThrowWhenFileRecordNotFound() {
        when(fileRecordMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> localFileService.getFile(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("文件不存在");
    }

    private byte[] pngBytes() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jp0QAAAAASUVORK5CYII="
        );
    }
}
