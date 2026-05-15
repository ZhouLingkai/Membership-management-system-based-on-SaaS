package com.ecards.member_management.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * forbiddenDays字段校验器实现
 */
public class ForbiddenDaysConstraintValidator implements ConstraintValidator<ValidForbiddenDays, List<String>> {

    @Override
    public void initialize(ValidForbiddenDays constraintAnnotation) {
        // 初始化方法，可以为空
    }

    @Override
    public boolean isValid(List<String> forbiddenDays, ConstraintValidatorContext context) {
        // null或空列表视为有效
        if (forbiddenDays == null || forbiddenDays.isEmpty()) {
            return true;
        }

        // 调用工具类进行校验
        String errorMessage = ForbiddenDaysValidator.validate(forbiddenDays);
        
        if (errorMessage != null) {
            // 校验失败，自定义错误消息
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
            return false;
        }

        return true; // 校验通过
    }
}
