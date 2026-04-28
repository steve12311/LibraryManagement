package org.dwtech.common.annontation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.dwtech.common.core.validator.FieldValidator;

import java.lang.annotation.*;

/**
 * 用于验证字段值是否合法的注解
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Documented
@Constraint(validatedBy = FieldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidField {

    /**
     * 验证失败时的错误信息。
     *
     * @return 验证失败时的错误提示信息
     */
    String message() default "非法字段";

    /**
     * 校验分组。
     *
     * @return 分组类数组
     */
    Class<?>[] groups() default {};

    /**
     * 校验负载。
     *
     * @return 负载类数组
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 允许的合法值列表。
     *
     * @return 允许的合法值数组
     */
    String[] allowedValues();

}
