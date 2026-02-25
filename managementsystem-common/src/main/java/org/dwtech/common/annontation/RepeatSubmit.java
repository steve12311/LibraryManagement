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
     * 用途：执行 expire 操作。
     * 
     * 锁过期时间（秒）
     * <p>
     * 默认5秒内不允许重复提交
     * 
     * 入参：无。
     * @return 数值结果
     */
    int expire() default 5;

}
