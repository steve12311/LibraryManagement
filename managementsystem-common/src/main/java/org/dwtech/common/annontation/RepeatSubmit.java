package org.dwtech.common.annontation;

import java.lang.annotation.*;
/**
 * RepeatSubmit
 *
 * @author steve12311
 * @since 2026-02-12
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RepeatSubmit {

    /**
     * 锁过期时间（秒）
     * <p>
     * 默认5秒内不允许重复提交
     */
    int expire() default 5;

}
