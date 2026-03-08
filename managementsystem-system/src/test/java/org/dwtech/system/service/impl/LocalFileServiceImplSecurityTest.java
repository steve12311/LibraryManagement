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
