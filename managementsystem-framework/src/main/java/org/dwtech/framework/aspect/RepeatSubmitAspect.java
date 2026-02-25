package org.dwtech.framework.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.dwtech.common.annontation.RepeatSubmit;
import org.dwtech.common.constant.RedisConstants;
import org.dwtech.common.constant.SecurityConstants;
import org.dwtech.common.enmus.ResultCode;
import org.dwtech.common.exception.BusinessException;
import org.dwtech.common.utils.IPUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
/**
 * RepeatSubmitAspect
 *
 * @author steve12311
 * @since 2026-02-12
 */

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RepeatSubmitAspect {
    private final RedissonClient redissonClient;

    /**
     * 定义防重复提交切点，拦截被 {@link RepeatSubmit} 标注的方法。
     *
     * @param repeatSubmit 防重复提交注解实例
     */
    @Pointcut("@annotation(repeatSubmit)")
    public void repeatSubmitPointCut(RepeatSubmit repeatSubmit) {
    }

    /**
     * 环绕通知：通过分布式锁拦截重复提交请求。
     *
     * <p>按注解配置的过期时间尝试获取锁，获取失败表示同一用户在短时间内重复提交，
     * 直接抛出业务异常；获取成功后放行业务方法。</p>
     *
     * @param pjp 连接点
     * @param repeatSubmit 防重复提交注解参数
     * @return 原业务方法执行结果
     * @throws Throwable 目标方法执行异常
     */
    @Around(value = "repeatSubmitPointCut(repeatSubmit)", argNames = "pjp,repeatSubmit")
    public Object handleRepeatSubmit(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String lockKey = buildLockKey();

        int expire = repeatSubmit.expire();
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = lock.tryLock(0, expire, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(ResultCode.USER_DUPLICATE_REQUEST);
        }
        return pjp.proceed();
    }

    /**
     * 生成防重复提交锁键。
     *
     * <p>键由“用户标识 + 请求方法 + 请求路径”组成，确保同一用户访问同一路径时具备幂等保护。</p>
     *
     * @return 分布式锁键
     */
    private String buildLockKey() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // 用户唯一标识
        String userIdentifier = getUserIdentifier(request);
        // 请求唯一标识 = 请求方法 + 请求路径 + 请求参数(严谨的做法)
        String requestIdentifier = StrUtil.join(":", request.getMethod(), request.getRequestURI());
        return StrUtil.format(RedisConstants.Lock.RESUBMIT, userIdentifier, requestIdentifier);
    }

    /**
     * 提取当前请求的用户标识。
     *
     * <p>优先使用 Bearer Token（取原始 token 后做 SHA-256 摘要）作为用户标识，
     * 无 token 时降级使用客户端 IP，兼容未登录或匿名访问场景。</p>
     *
     * @param request HTTP 请求对象
     * @return 用户唯一标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // 用户身份唯一标识
        String userIdentifier;
        // 从请求头中获取 Token
        String tokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(tokenHeader) && tokenHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            String rawToken = tokenHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());  // 去掉 Bearer 后的 Token
            userIdentifier = DigestUtil.sha256Hex(rawToken); // 使用 SHA-256 加密 Token 作为用户唯一标识
        } else {
            userIdentifier = IPUtils.getIpAddr(request); // 使用 IP 作为用户唯一标识
        }
        return userIdentifier;
    }
}
