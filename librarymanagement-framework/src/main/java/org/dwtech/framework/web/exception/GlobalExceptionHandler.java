package org.dwtech.framework.web.exception;

import org.dwtech.common.core.entity.AjaxResult;
import org.dwtech.common.exception.ServiceException;
import org.dwtech.common.exception.NotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e) {
        return AjaxResult.error(org.dwtech.common.constant.HttpStatus.ERROR, e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return AjaxResult.error(org.dwtech.common.constant.HttpStatus.BAD_REQUEST, String.join("，", errors));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NotValidException.class)
    public AjaxResult handleUnUniqueException(NotValidException e) {
        return AjaxResult.error(org.dwtech.common.constant.HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
