package org.dwtech.system.service.impl;

import org.dwtech.system.mapper.FileObjectMapper;
import org.dwtech.system.mapper.FileRecordMapper;
import org.dwtech.system.model.entity.FileObjectPO;
import org.dwtech.system.model.entity.FileRecordPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFileServiceImplSecurityTest {

    @Mock
    private FileObjectMapper fileObjectMapper;

    @Mock
    private FileRecordMapper fileRecordMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @TempDir
    Path tempDir;

    private LocalFileServiceImpl localFileService;

    @BeforeEach
    void setUp() {
        localFileService = new LocalFileServiceImpl(fileObjectMapper, fileRecordMapper, redisTemplate);
        ReflectionTestUtils.setField(localFileService, "storagePath", tempDir.toString());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        setRootUser();
    }

    @Test
    void shouldRejectPathTraversalWhenGetFile() {
        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(1L);
        fileRecordPO.setObjectId(2L);
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(1L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(2L);
        fileObjectPO.setStoragePath("../outside.txt");
        when(fileObjectMapper.selectById(2L)).thenReturn(fileObjectPO);

        assertThatThrownBy(() -> localFileService.getFile(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("非法文件路径");
    }

    @Test
    void shouldReturnFalseWhenDeletePhysicalFileIdIsNull() {
        boolean deleted = localFileService.deleteFilePhysical(null);
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDeleteByRefCountFileIdIsNull() {
        boolean deleted = localFileService.deleteFileByRefCount(null);
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldAllowAnonymousUserToReadAnyFile() throws Exception {
        SecurityContextHolder.clearContext();
        Path objectPath = tempDir.resolve(".objects/aa/bb/any-file");
        java.nio.file.Files.createDirectories(objectPath.getParent());
        java.nio.file.Files.writeString(objectPath, "file-bytes");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(4L);
        fileRecordPO.setObjectId(5L);
        fileRecordPO.setOriginalName("any-file.png");
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(4L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(5L);
        fileObjectPO.setStoragePath(".objects/aa/bb/any-file");
        fileObjectPO.setMimeType("image/png");
        fileObjectPO.setFileSize(13L);
        fileObjectPO.setSha256("any-sha");
        when(fileObjectMapper.selectById(5L)).thenReturn(fileObjectPO);

        org.dwtech.system.model.bo.FileDownloadBO fileDownloadBO = localFileService.getFile(4L);

        assertThat(fileDownloadBO.getFilePath()).isEqualTo(objectPath.toAbsolutePath().normalize());
        assertThat(fileDownloadBO.getFileName()).isEqualTo("any-file.png");
    }

    @Test
    void shouldAllowRootUserToReadAnyFile() throws Exception {
        Path objectPath = tempDir.resolve(".objects/aa/bb/root-read");
        java.nio.file.Files.createDirectories(objectPath.getParent());
        java.nio.file.Files.writeString(objectPath, "root-bytes");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(7L);
        fileRecordPO.setObjectId(8L);
        fileRecordPO.setOriginalName("private-file.png");
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(7L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(8L);
        fileObjectPO.setStoragePath(".objects/aa/bb/root-read");
        fileObjectPO.setMimeType("image/png");
        fileObjectPO.setFileSize(10L);
        fileObjectPO.setSha256("root-sha");
        when(fileObjectMapper.selectById(8L)).thenReturn(fileObjectPO);

        org.dwtech.system.model.bo.FileDownloadBO fileDownloadBO = localFileService.getFile(7L);

        assertThat(fileDownloadBO.getFilePath()).isEqualTo(objectPath.toAbsolutePath().normalize());
    }

    private void setRootUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "root",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ROOT"))
                )
        );
    }

    private void setUser(Long userId) {
        org.dwtech.common.core.entity.SysUserDetails userDetails = new org.dwtech.common.core.entity.SysUserDetails();
        userDetails.setUserId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }
}
