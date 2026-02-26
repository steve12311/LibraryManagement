package org.dwtech.system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileServiceImplSecurityTest {

    @TempDir
    Path tempDir;

    private LocalFileServiceImpl localFileService;

    @BeforeEach
    void setUp() {
        localFileService = new LocalFileServiceImpl();
        ReflectionTestUtils.setField(localFileService, "storagePath", tempDir.toString());
    }

    @Test
    void shouldRejectPathTraversalWhenDeleteFile() throws Exception {
        Path outsideFile = Files.createTempFile("outside-", ".txt");
        Files.writeString(outsideFile, "do-not-delete");

        boolean deleted = localFileService.deleteFile("/../" + outsideFile.getFileName());

        assertThat(deleted).isFalse();
        assertThat(Files.exists(outsideFile)).isTrue();

        Files.deleteIfExists(outsideFile);
    }
}
