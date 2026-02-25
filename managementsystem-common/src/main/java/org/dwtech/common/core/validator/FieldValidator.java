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
     * 用途：初始化 ialize。
     * 
     * @param constraintAnnotation constraint annotation
     * 返回：无。
     */
    @Override
    public void initialize(ValidField constraintAnnotation) {
        // 初始化允许的值列表
        this.allowedValues = constraintAnnotation.allowedValues();
    }

    /**
     * 用途：判断 valid 状态。
     * 
     * @param value value
     * @param context context
     * @return 操作结果，true 表示成功，false 表示失败
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
