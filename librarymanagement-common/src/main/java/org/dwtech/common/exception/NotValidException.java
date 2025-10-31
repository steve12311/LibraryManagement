package org.dwtech.common.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;

@NoArgsConstructor
public final class NotValidException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    @Getter
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     */
    @Getter
    private String detailMessage;

    public NotValidException(String message) {
        this.message = message;
    }

    public NotValidException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public NotValidException setMessage(String message) {
        this.message = message;
        return this;
    }

    public NotValidException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}
