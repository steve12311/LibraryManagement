package org.dwtech.common.exception;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.enmus.ResultCode;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/**
 * GlobalExceptionHandler
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 用途：处理 exception。
     * 
     * 处理绑定异常
     * <p>
     * 当请求参数绑定到对象时发生错误，会抛出 BindException 异常。
     * 
     * @param e e
     * @return 返回结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(BindException e) {
        String msg = e.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；"));
        log.warn("客户端异常, exceptionType={}, resultCode={}, message={}",
                BindException.class.getSimpleName(),
                ResultCode.USER_REQUEST_PARAMETER_ERROR.getCode(),
                sanitizeMessage(msg));
        return Result.failed(ResultCode.USER_REQUEST_PARAMETER_ERROR, msg);
    }

    /**
     * 用途：处理 exception。
     * 
     * 处理 @RequestParam 参数校验异常
     * <p>
     * 当请求参数在校验过程中发生违反约束条件的异常时（如 @RequestParam 验证不通过），
     * 会捕获到 ConstraintViolationException 异常。
     * 
     * @param e e
     * @return 返回结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("；"));
        log.warn("客户端异常, exceptionType={}, resultCode={}, message={}",
                ConstraintViolationException.class.getSimpleName(),
                ResultCode.INVALID_USER_INPUT.getCode(),
                sanitizeMessage(msg));
        return Result.failed(ResultCode.INVALID_USER_INPUT, msg);
    }

    /**
     * 用途：处理 exception。
     * 
     * 处理方法参数校验异常
     * <p>
     * 当使用 @Valid 或 @Validated 注解对方法参数进行验证时，如果验证失败，
     * 会抛出 MethodArgumentNotValidException 异常。
     * 
     * @param e e
     * @return 返回结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；"));
        log.warn("客户端异常, exceptionType={}, resultCode={}, message={}",
                MethodArgumentNotValidException.class.getSimpleName(),
                ResultCode.INVALID_USER_INPUT.getCode(),
                sanitizeMessage(msg));
        return Result.failed(ResultCode.INVALID_USER_INPUT, msg);
    }

    /**
     * 用途：处理 exception。
     * 
     * 处理请求体不可读的异常
     * <p>
     * 当请求体不可读时，会抛出 HttpMessageNotReadableException 异常。
     * 
     * @param e e
     * @return 返回结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(HttpMessageNotReadableException e) {
        String errorMessage = "请求体不可为空";
        Throwable cause = e.getCause();
        if (cause != null) {
            errorMessage = convertMessage(cause);
        }
        log.warn("客户端异常, exceptionType={}, resultCode={}, message={}",
                HttpMessageNotReadableException.class.getSimpleName(),
                ResultCode.SYSTEM_ERROR.getCode(),
                sanitizeMessage(errorMessage));
        return Result.failed(errorMessage);
    }

    /**
     * 用途：处理 biz exception。
     * 
     * 处理业务异常
     * <p>
     * 当业务逻辑发生错误时，会抛出 BusinessException 异常。
     * 
     * @param e e
     * @return 返回结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> handleBizException(BusinessException e) {
        log.warn("业务异常, exceptionType={}, resultCode={}",
                BusinessException.class.getSimpleName(),
                resolveResultCode(e));
        if (e.getResultCode() != null) {
            return Result.failed(e.getResultCode(), e.getMessage());
        }
        return Result.failed(e.getMessage());
    }

    /**
     * 用途：处理 exception。
     * 
     * 处理所有未捕获的异常
     * <p>
     * 当发生未捕获的异常时，会抛出 Exception 异常。
     * 
     * @param e e
     * @return 返回结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> handleException(Exception e) throws Exception {
        // 将 Spring Security 异常继续抛出，以便交给自定义处理器处理
        if (e instanceof AccessDeniedException
                || e instanceof AuthenticationException) {
            throw e;
        }
        log.error("unknown exception", e);
        return Result.failed("系统繁忙，请稍后再试");
    }

    /**
     * 用途：转换 message。
     * 
     * 传参类型错误时，用于消息转换
     *
     * @param throwable 异常
     * @return 错误信息
     */
    private String convertMessage(Throwable throwable) {
        String error = throwable.toString();
        String regulation = "\\[\"(.*?)\"]+";
        Pattern pattern = Pattern.compile(regulation);
        Matcher matcher = pattern.matcher(error);
        String group = "";
        if (matcher.find()) {
            String matchString = matcher.group();
            matchString = matchString.replace("[", "").replace("]", "");
            matchString = "%s字段类型错误".formatted(matchString.replaceAll("\"", ""));
            group += matchString;
        }
        return group;
    }

    /**
     * 获取业务异常的结果码，避免日志格式不一致。
     *
     * @param exception 业务异常
     * @return 结果码
     */
    private String resolveResultCode(BusinessException exception) {
        if (exception.getResultCode() == null) {
            return "UNKNOWN";
        }
        return exception.getResultCode().getCode();
    }

    /**
     * 清洗日志消息，避免空值与过长文本污染结构化日志。
     *
     * @param message 原始消息
     * @return 清洗后的消息
     */
    private String sanitizeMessage(String message) {
        if (StrUtil.isBlank(message)) {
            return "unknown";
        }
        return StrUtil.maxLength(message.replaceAll("[\\r\\n]+", " "), 120);
    }
}
