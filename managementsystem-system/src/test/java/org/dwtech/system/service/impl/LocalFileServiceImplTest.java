package org.dwtech.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dwtech.common.core.entity.FileInfo;
import org.dwtech.system.mapper.FileObjectMapper;
import org.dwtech.system.mapper.FileRecordMapper;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.dwtech.system.model.entity.FileObjectPO;
import org.dwtech.system.model.entity.FileRecordPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

    private LocalFileServiceImpl localFileService;

    @BeforeEach
    void setUp() {
        localFileService = new LocalFileServiceImpl(fileObjectMapper, fileRecordMapper);
        ReflectionTestUtils.setField(localFileService, "storagePath", tempDir.toString());
    }

    @Test
    void shouldUploadFileAndReturnFileIdUrlWhenObjectNotExists() throws Exception {
        byte[] content = "hello-library".getBytes(StandardCharsets.UTF_8);
        String expectedSha256 = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "book.txt",
                "text/plain",
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

        assertThat(fileInfo.getName()).isEqualTo("book.txt");
        assertThat(fileInfo.getUrl()).isEqualTo("/101");

        ArgumentCaptor<FileObjectPO> objectCaptor = ArgumentCaptor.forClass(FileObjectPO.class);
        verify(fileObjectMapper).insert(objectCaptor.capture());
        FileObjectPO insertedObject = objectCaptor.getValue();
        assertThat(insertedObject.getSha256()).isEqualTo(expectedSha256);
        assertThat(insertedObject.getRefCount()).isEqualTo(1);
        assertThat(insertedObject.getStoragePath()).startsWith(".objects/");
        Path storedFile = tempDir.resolve(insertedObject.getStoragePath());
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readString(storedFile)).isEqualTo("hello-library");

        ArgumentCaptor<FileRecordPO> recordCaptor = ArgumentCaptor.forClass(FileRecordPO.class);
        verify(fileRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getObjectId()).isEqualTo(11L);
        assertThat(recordCaptor.getValue().getOriginalName()).isEqualTo("book.txt");
    }

    @Test
    void shouldReuseObjectWhenUploadDuplicateFile() {
        byte[] content = "same-content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile1 = new MockMultipartFile(
                "file",
                "a.txt",
                "text/plain",
                content
        );
        MockMultipartFile multipartFile2 = new MockMultipartFile(
                "file",
                "b.txt",
                "text/plain",
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
    void shouldGetFileByFileId() throws Exception {
        Path objectPath = tempDir.resolve(".objects/aa/bb/abcdef");
        Files.createDirectories(objectPath.getParent());
        Files.writeString(objectPath, "img-content");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(9L);
        fileRecordPO.setObjectId(3L);
        fileRecordPO.setOriginalName("cover.png");
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
    }

    @Test
    void shouldDeleteFileAndPhysicalObjectWhenRefCountReachZero() throws Exception {
        Path objectPath = tempDir.resolve(".objects/aa/bb/to-delete");
        Files.createDirectories(objectPath.getParent());
        Files.writeString(objectPath, "will-delete");

        FileRecordPO fileRecordPO = new FileRecordPO();
        fileRecordPO.setId(7L);
        fileRecordPO.setObjectId(88L);
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
    }

    @Test
    void shouldThrowWhenFileRecordNotFound() {
        when(fileRecordMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> localFileService.getFile(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("文件不存在");
    }
}
