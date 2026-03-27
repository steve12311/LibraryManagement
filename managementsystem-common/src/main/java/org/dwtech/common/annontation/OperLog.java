package org.dwtech.common.annontation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 *
 * @author steve12311
 * @since 2026-03-27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 审计模块名称。
     *
     * @return 模块名称
     */
    String module();

    /**
     * 审计动作名称。
     *
     * @return 动作名称
     */
    String action();

    /**
     * 业务资源标识表达式，默认使用空字符串表示不记录。
     *
     * <p>表达式使用 {@code #p0}、{@code #p1} 这类位置参数，避免依赖编译参数名保留。</p>
     *
     * @return 业务资源标识表达式
     */
    String bizId() default "";
}
