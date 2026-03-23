package org.dwtech.controller;

import cn.hutool.captcha.generator.CodeGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dwtech.common.config.properties.SecurityProperties;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.token.TokenManager;
import org.dwtech.framework.ai.AISearchService;
import org.dwtech.framework.config.SecurityConfig;
import org.dwtech.framework.config.WebMvcConfig;
import org.dwtech.framework.security.service.SysUserDetailService;
import org.dwtech.system.model.query.PublicBookPageQuery;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.dwtech.system.service.StockService;
import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IndexController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfig.class)
)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, IndexControllerIntegrationTest.TestSecurityPropertiesConfig.class})
class IndexControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AISearchService aiSearchService;

    @MockitoBean
    private StockService stockService;

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
    void shouldAllowAnonymousToGetPublicBookPage() throws Exception {
        PublicBookPageVO book = new PublicBookPageVO();
        book.setCoverUrl("/api/v1/files/file-1");
        book.setName("Spring Boot 实战");
        book.setIsbn("9787300000001");
        book.setAvailable(true);
        book.setIntro("一本用于公开展示的图书简介");
        book.setCategoryName("后端开发");
        book.setPublishName("电子工业出版社");
        book.setPublishTime(new Date(0));
        book.setPrice(new BigDecimal("68.00"));
        book.setAuthor("张三");

        Page<PublicBookPageVO> page = new Page<>(1, 10);
        page.setRecords(List.of(book));
        page.setTotal(1);
        when(stockService.getPublicBookPage(any(PublicBookPageQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/index/books/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("field", "name")
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].coverUrl").value("/api/v1/files/file-1"))
                .andExpect(jsonPath("$.data.list[0].name").value("Spring Boot 实战"))
                .andExpect(jsonPath("$.data.list[0].isbn").value("9787300000001"))
                .andExpect(jsonPath("$.data.list[0].available").value(true))
                .andExpect(jsonPath("$.data.list[0].categoryName").value("后端开发"))
                .andExpect(jsonPath("$.data.list[0].publishName").value("电子工业出版社"))
                .andExpect(jsonPath("$.data.list[0].price").value(68.00))
                .andExpect(jsonPath("$.data.list[0].stockNumber").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].currentNumber").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].createTime").doesNotExist());

        verify(stockService).getPublicBookPage(any(PublicBookPageQuery.class));
        verifyNoInteractions(tokenManager);
    }

    @Test
    void shouldRejectInvalidPaginationBoundaryForPublicBookPage() throws Exception {
        mockMvc.perform(get("/api/v1/index/books/page")
                        .param("pageNum", "0")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_USER_INPUT.getCode()))
                .andExpect(jsonPath("$.msg", containsString("页码必须大于等于 1")))
                .andExpect(jsonPath("$.msg", containsString("每页条数不能超过 100")));

        verifyNoInteractions(stockService);
        verifyNoInteractions(tokenManager);
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
            securityProperties.setIgnoreUrls(new String[]{
                    "/api/v1/auth/**",
                    "/api/v1/index/books/page"
            });
            securityProperties.setUnsecuredUrls(new String[]{"/doc.html"});
            return securityProperties;
        }
    }
}
