package org.dwtech.controller;

import cn.hutool.captcha.generator.CodeGenerator;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.token.TokenManager;
import org.dwtech.framework.config.SecurityConfig;
import org.dwtech.framework.config.WebMvcConfig;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.system.model.bo.FileDownloadBO;
import org.dwtech.system.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FileController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfig.class)
)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, FileControllerIntegrationTest.TestSecurityPropertiesConfig.class})
class FileControllerIntegrationTest {

    @TempDir
    Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private TokenManager tokenManager;

    @MockitoBean
    private SysUserDetailService sysUserDetailService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CodeGenerator codeGenerator;

    @Test
    void shouldAllowAnonymousGetFile() throws Exception {
        Path filePath = tempDir.resolve("cover.png");
        Files.writeString(filePath, "cover");

        FileDownloadBO fileDownloadBO = new FileDownloadBO();
        fileDownloadBO.setFilePath(filePath);
        fileDownloadBO.setFileName("cover.png");
        fileDownloadBO.setMimeType("image/png");
        fileDownloadBO.setFileSize(Files.size(filePath));
        when(fileService.getFile(9L)).thenReturn(fileDownloadBO);

        mockMvc.perform(get("/api/v1/files/9"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("cover.png")));

        verify(fileService).getFile(9L);
    }

    @TestConfiguration
    static class TestSecurityPropertiesConfig {

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties securityProperties = new SecurityProperties();

            SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
            sessionConfig.setType("jwt");
            securityProperties.setSession(sessionConfig);

            SecurityProperties.CorsConfig corsConfig = new SecurityProperties.CorsConfig();
            securityProperties.setCors(corsConfig);
            securityProperties.setIgnoreUrls(new String[]{"/api/v1/auth/**", "/api/v1/index/books/page"});
            securityProperties.setUnsecuredUrls(new String[]{"/doc.html"});
            return securityProperties;
        }
    }
}
