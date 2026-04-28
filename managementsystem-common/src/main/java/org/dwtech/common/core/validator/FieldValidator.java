package org.dwtech.common.core.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dwtech.common.annontation.ValidField;

import java.util.Arrays;

/**
 * 字段校验器
 *
 * @author steve12311
 * @since 2025-11-18
 */
public class FieldValidator implements ConstraintValidator<ValidField, String> {

    private String[] allowedValues;

    /**
     * 初始化校验器，从注解中获取允许的合法值列表。
     *
     * @param constraintAnnotation 验证字段注解
     */
    @Override
    public void initialize(ValidField constraintAnnotation) {
        // 初始化允许的值列表
        this.allowedValues = constraintAnnotation.allowedValues();
    }

    /**
     * 校验字段值是否在允许的合法值列表中。
     *
     * @param value   待校验字段值
     * @param context 校验上下文
     * @return 值合法返回 true，否则返回 false
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 如果字段允许为空，可以返回 true
        }
        // 检查值是否在允许列表中
        return Arrays.asList(allowedValues).contains(value);
    }
}
