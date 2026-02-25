package org.dwtech.common.annontation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DataPermission {

    /**
     * 用途：执行 dept alias 操作。
     * 
     * 数据权限 {@link com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor}
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String deptAlias() default "";

    /**
     * 用途：执行 dept id column name 操作。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String deptIdColumnName() default "dept_id";

    /**
     * 用途：执行 user alias 操作。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String userAlias() default "";

    /**
     * 用途：执行 user id column name 操作。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String userIdColumnName() default "create_by";

}

