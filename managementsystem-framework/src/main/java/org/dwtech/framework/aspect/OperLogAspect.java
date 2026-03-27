package org.dwtech.framework.aspect;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.dwtech.common.annontation.OperLog;
import org.dwtech.common.core.entity.IResultCode;
import org.dwtech.common.core.entity.Result;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.IPUtils;
import org.dwtech.common.utils.SecurityUtils;
import org.dwtech.common.utils.ServletUtils;
import org.dwtech.system.model.entity.OperLogPO;
import org.dwtech.system.service.OperLogService;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.Method;

/**
 * 操作审计日志切面
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {
    private static final int MAX_ERROR_SUMMARY_LENGTH = 200;

    private final OperLogService operLogService;

    @Around(value = "@annotation(operLog)", argNames = "pjp,operLog")
    public Object recordOperLog(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        OperLogPO logEntry = buildBaseLog(pjp, operLog);
        try {
            Object result = pjp.proceed();
            fillResult(logEntry, result);
            operLogService.saveQuietly(logEntry);
            return result;
        } catch (Throwable ex) {
            fillException(logEntry, ex);
            operLogService.saveQuietly(logEntry);
            throw ex;
        }
    }

    private OperLogPO buildBaseLog(ProceedingJoinPoint pjp, OperLog operLog) {
        OperLogPO logEntry = new OperLogPO();
        logEntry.setModule(operLog.module());
        logEntry.setAction(operLog.action());
        logEntry.setBizResourceId(resolveBizId(pjp, operLog.bizId()));
        logEntry.setOperatorUserId(SecurityUtils.getUserId());
        logEntry.setOperatorUsername(SecurityUtils.getUsername());
        logEntry.setClientIp(resolveClientIp());
        HttpServletRequest request = resolveRequest();
        if (request != null) {
            logEntry.setRequestMethod(request.getMethod());
            logEntry.setRequestPath(request.getRequestURI());
        } else {
            logEntry.setRequestMethod("UNKNOWN");
            logEntry.setRequestPath("UNKNOWN");
        }
        return logEntry;
    }

    private void fillResult(OperLogPO logEntry, Object result) {
        if (result instanceof Result<?> response && !Result.isSuccess(response)) {
            logEntry.setSuccess(0);
            logEntry.setResultCode(response.getCode());
            logEntry.setErrorSummary(truncateSummary(response.getMsg()));
            return;
        }
        logEntry.setSuccess(1);
        logEntry.setResultCode(ResultCode.SUCCESS.getCode());
    }

    private void fillException(OperLogPO logEntry, Throwable ex) {
        logEntry.setSuccess(0);
        if (ex instanceof BusinessException businessException) {
            IResultCode resultCode = businessException.getResultCode();
            logEntry.setResultCode(resultCode == null ? ResultCode.SYSTEM_ERROR.getCode() : resultCode.getCode());
            logEntry.setErrorSummary(truncateSummary(businessException.getMessage()));
            return;
        }
        logEntry.setResultCode(ResultCode.SYSTEM_ERROR.getCode());
        logEntry.setErrorSummary(truncateSummary("系统异常:" + ex.getClass().getSimpleName()));
    }

    private String resolveBizId(ProceedingJoinPoint pjp, String expression) {
        if (StrUtil.isBlank(expression)) {
            return null;
        }
        try {
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            StandardEvaluationContext context = new StandardEvaluationContext();
            Object[] args = pjp.getArgs();
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
            context.setVariable("method", method);
            ExpressionParser parser = new SpelExpressionParser();
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("解析操作日志业务标识失败, expression={}, exceptionType={}",
                    expression, e.getClass().getSimpleName());
            return null;
        }
    }

    private String resolveClientIp() {
        HttpServletRequest request = resolveRequest();
        return request == null ? "unknown" : StrUtil.blankToDefault(IPUtils.getIpAddr(request), "unknown");
    }

    private HttpServletRequest resolveRequest() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return null;
        }
        return ServletUtils.getRequest();
    }

    private String truncateSummary(String summary) {
        if (StrUtil.isBlank(summary)) {
            return null;
        }
        return StrUtil.maxLength(summary, MAX_ERROR_SUMMARY_LENGTH);
    }
}
