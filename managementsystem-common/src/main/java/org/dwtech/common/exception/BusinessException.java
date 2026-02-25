package org.dwtech.common.exception;

import org.dwtech.common.core.entity.IResultCode;
import lombok.Getter;
import org.slf4j.helpers.MessageFormatter;

/**
 * 自定义业务异常
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Getter
public class BusinessException extends RuntimeException {

    public IResultCode resultCode;

    /**
     * 用途：创建 BusinessException 实例。
     * 
     * @param errorCode error code
     * 返回：无。
     */
    public BusinessException(IResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }


    /**
     * 用途：创建 BusinessException 实例。
     * 
     * @param errorCode error code
     * @param message message
     * 返回：无。
     */
    public BusinessException(IResultCode errorCode, String message) {
        super(message);
        this.resultCode = errorCode;
    }


    /**
     * 用途：创建 BusinessException 实例。
     * 
     * @param message message
     * @param cause cause
     * 返回：无。
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 用途：创建 BusinessException 实例。
     * 
     * @param cause cause
     * 返回：无。
     */
    public BusinessException(Throwable cause) {
        super(cause);
    }

    /**
     * 用途：创建 BusinessException 实例。
     * 
     * @param message message
     * @param args args
     * 返回：无。
     */
    public BusinessException(String message, Object... args) {
        super(formatMessage(message, args));
    }

    /**
     * 用途：执行 format message 操作。
     * 
     * @param message message
     * @param args args
     * @return 结果字符串
     */
    private static String formatMessage(String message, Object... args) {
        return MessageFormatter.arrayFormat(message, args).getMessage();
    }
}
