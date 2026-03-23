package org.dwtech.framework.security.service;

import org.dwtech.system.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SysUserDetailServiceTest {

    @Mock
    private UserService userService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldLogStructuredFailureWithoutLeakingExceptionMessage(CapturedOutput output) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-forwarded-for", "10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(userService.getAuthCredentialsByUsername("alice"))
                .thenThrow(new RuntimeException("db secret message"));

        SysUserDetailService service = new SysUserDetailService(userService);

        assertThrows(RuntimeException.class, () -> service.loadUserByUsername("alice"));

        assertThat(output).contains("action=load_user");
        assertThat(output).contains("username=alice");
        assertThat(output).doesNotContain("db secret message");
    }
}
