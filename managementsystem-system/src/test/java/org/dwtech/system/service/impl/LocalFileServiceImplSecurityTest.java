package org.dwtech.system.service.impl;

import org.dwtech.system.mapper.FileObjectMapper;
import org.dwtech.system.mapper.FileRecordMapper;
import org.dwtech.system.mapper.BookMapper;
import org.dwtech.system.model.entity.FileObjectPO;
import org.dwtech.system.model.entity.FileRecordPO;
import org.dwtech.common.service.PermissionService;
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
    private BookMapper bookMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @TempDir
    Path tempDir;

    private LocalFileServiceImpl localFileService;

    @BeforeEach
    void setUp() {
        localFileService = new LocalFileServiceImpl(fileObjectMapper, fileRecordMapper, bookMapper, permissionService, redisTemplate);
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
    void shouldReturnFalseWhenDeleteFileIdIsNull() {
        boolean deleted = localFileService.deleteFile(null);
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldRejectGetFileWhenCurrentUserIsNotOwner() {
        setUser(2002L);
        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(2L);
        fileRecordPO.setObjectId(3L);
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(2L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(3L);
        fileObjectPO.setStoragePath(".objects/aa/bb/exists");
        when(fileObjectMapper.selectById(3L)).thenReturn(fileObjectPO);

        assertThatThrownBy(() -> localFileService.getFile(2L))
                .isInstanceOf(org.dwtech.common.exception.BusinessException.class)
                .hasMessage("无权访问该文件");
    }

    @Test
    void shouldRejectDeleteFileWhenCurrentUserIsNotOwner() {
        setUser(2002L);
        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(3L);
        fileRecordPO.setObjectId(4L);
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(3L)).thenReturn(fileRecordPO);

        assertThatThrownBy(() -> localFileService.deleteFile(3L))
                .isInstanceOf(org.dwtech.common.exception.BusinessException.class)
                .hasMessage("无权访问该文件");
    }

    @Test
    void shouldAllowPublicBookCoverReadForAnonymousUser() throws Exception {
        SecurityContextHolder.clearContext();
        Path objectPath = tempDir.resolve(".objects/aa/bb/public-cover");
        java.nio.file.Files.createDirectories(objectPath.getParent());
        java.nio.file.Files.writeString(objectPath, "cover-bytes");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(4L);
        fileRecordPO.setObjectId(5L);
        fileRecordPO.setOriginalName("cover.png");
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(4L)).thenReturn(fileRecordPO);

        FileObjectPO fileObjectPO = new FileObjectPO();
        fileObjectPO.setId(5L);
        fileObjectPO.setStoragePath(".objects/aa/bb/public-cover");
        fileObjectPO.setMimeType("image/png");
        fileObjectPO.setFileSize(11L);
        fileObjectPO.setSha256("cover-sha");
        when(fileObjectMapper.selectById(5L)).thenReturn(fileObjectPO);
        when(bookMapper.selectCount(any())).thenReturn(1L);

        org.dwtech.system.model.bo.FileDownloadBO fileDownloadBO = localFileService.getFile(4L);

        assertThat(fileDownloadBO.getFilePath()).isEqualTo(objectPath.toAbsolutePath().normalize());
        assertThat(fileDownloadBO.getFileName()).isEqualTo("cover.png");
    }

    @Test
    void shouldRejectDeleteBookCoverWhenUserLacksStockEditPermission() {
        setUser(2002L);
        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(5L);
        fileRecordPO.setObjectId(6L);
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(5L)).thenReturn(fileRecordPO);
        when(bookMapper.selectCount(any())).thenReturn(1L);
        when(permissionService.hasPerm("sys:stock:edit")).thenReturn(false);

        assertThatThrownBy(() -> localFileService.deleteFile(5L))
                .isInstanceOf(org.dwtech.common.exception.BusinessException.class)
                .hasMessage("无权删除书籍封面文件");
    }

    @Test
    void shouldAllowDeleteBookCoverWhenUserHasStockEditPermission() {
        setUser(2002L);
        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(6L);
        fileRecordPO.setOwnerUserId(1001L);
        when(fileRecordMapper.selectById(6L)).thenReturn(fileRecordPO);
        when(bookMapper.selectCount(any())).thenReturn(1L, 0L);
        when(permissionService.hasPerm("sys:stock:edit")).thenReturn(true);
        when(fileRecordMapper.deleteById(6L)).thenReturn(1);

        boolean deleted = localFileService.deleteFile(6L);

        assertThat(deleted).isTrue();
        org.mockito.Mockito.verify(bookMapper).update(org.mockito.ArgumentMatchers.isNull(), any());
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
