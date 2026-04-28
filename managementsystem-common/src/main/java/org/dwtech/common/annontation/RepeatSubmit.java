package org.dwtech.common.annontation;

import java.lang.annotation.*;
/**
 * 防重复提交注解
 * <p>
 * 标注在写操作接口上，由 {@link org.dwtech.framework.aspect.RepeatSubmitAspect} 通过 Redisson 分布式锁实现。
 * 同一用户对同一接口在锁窗口内重复请求将被拒绝，锁粒度 = 用户标识 + 请求方法 + 请求路径。
 *
 * @author steve12311
 * @since 2026-02-12
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RepeatSubmit {

    /** 分布式锁过期时间（秒），默认 5 秒内不允许同一用户重复提交 */
    int expire() default 5;

}
