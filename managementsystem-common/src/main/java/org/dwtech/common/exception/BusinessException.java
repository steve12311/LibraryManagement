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
     * 根据业务错误码构造异常。
     *
     * @param errorCode 业务错误码
     */
    public BusinessException(IResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }


    /**
     * 根据业务错误码和自定义消息构造异常。
     *
     * @param errorCode 业务错误码
     * @param message   自定义错误消息
     */
    public BusinessException(IResultCode errorCode, String message) {
        super(message);
        this.resultCode = errorCode;
    }


    /**
     * 根据错误消息和异常原因构造异常。
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 根据原始异常构造异常。
     *
     * @param cause 原始异常
     */
    public BusinessException(Throwable cause) {
        super(cause);
    }

    /**
     * 根据带占位符的消息模板和参数构造异常。
     *
     * @param message 消息模板
     * @param args    参数列表
     */
    public BusinessException(String message, Object... args) {
        super(formatMessage(message, args));
    }

    /**
     * 格式化消息模板，替换占位符。
     *
     * @param message 消息模板
     * @param args    参数列表
     * @return 格式化后的消息
     */
    private static String formatMessage(String message, Object... args) {
        return MessageFormatter.arrayFormat(message, args).getMessage();
    }
}
