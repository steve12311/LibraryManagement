package org.dwtech.common.annontation;

import java.lang.annotation.*;

/**
 * 数据权限注解 — 行级数据隔离
 * <p>
 * 标注在 Mapper 方法上，由 {@link org.dwtech.common.plugin.MyDataPermissionHandler}
 * 根据当前用户的数据权限范围（全部/本部门/本部门及子部门/仅本人）自动拼接 SQL WHERE 条件。
 * 配合 MyBatis-Plus 的 {@code DataPermissionInterceptor} 使用。
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DataPermission {

    /** SQL 中部门表的别名，用于多表关联时区分 */
    String deptAlias() default "";

    /** 部门 ID 列名，默认 {@code dept_id} */
    String deptIdColumnName() default "dept_id";

    /** SQL 中用户表的别名，用于多表关联时区分 */
    String userAlias() default "";

    /** 用户 ID 列名（创建人），默认 {@code create_by} */
    String userIdColumnName() default "create_by";

}

