package org.dwtech.common.exception;

import org.dwtech.common.enmus.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldLogBusinessExceptionWithoutStackTraceOrSensitiveMessage(CapturedOutput output) {
        BusinessException exception = new BusinessException(ResultCode.REFRESH_TOKEN_INVALID, "refresh-secret-token invalid");

        handler.handleBizException(exception);

        assertThat(output).contains("业务异常");
        assertThat(output).contains(ResultCode.REFRESH_TOKEN_INVALID.getCode());
        assertThat(output).doesNotContain("at org.");
        assertThat(output).doesNotContain("refresh-secret-token invalid");
    }

    @Test
    void shouldLogHttpMessageNotReadableWithoutStackTrace(CapturedOutput output) {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "payload secret",
                new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8))
        );

        var result = handler.processException(exception);

        assertThat(output).contains("客户端异常");
        assertThat(output).contains(HttpMessageNotReadableException.class.getSimpleName());
        assertThat(output).contains(ResultCode.INVALID_USER_INPUT.getCode());
        assertThat(output).doesNotContain("at org.");
        assertThat(output).doesNotContain("payload secret");
        assertThat(result.getCode()).isEqualTo(ResultCode.INVALID_USER_INPUT.getCode());
        assertThat(result.getMsg()).isEqualTo("请求体不可为空");
    }

    @Test
    void shouldUseInternalServerErrorForUnhandledException() throws NoSuchMethodException {
        ResponseStatus responseStatus = GlobalExceptionHandler.class
                .getMethod("handleException", Exception.class)
                .getAnnotation(ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnSystemErrorForUnhandledException() throws Exception {
        RuntimeException exception = new RuntimeException("db-secret-message");

        var result = handler.handleException(exception);

        assertThat(result.getCode()).isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo("系统繁忙，请稍后再试");
    }
}
