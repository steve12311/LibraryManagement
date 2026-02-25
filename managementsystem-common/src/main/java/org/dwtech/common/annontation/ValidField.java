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
     * 用途：执行 message 操作。
     * 
     * 验证失败时的错误信息。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    String message() default "非法字段";

    /**
     * 用途：执行 groups 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    Class<?>[] groups() default {};

    /**
     * 用途：执行 payload 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 用途：执行 allowed values 操作。
     * 
     * 允许的合法值列表。
     * 
     * 入参：无。
     * @return 返回结果
     */
    String[] allowedValues();

}
