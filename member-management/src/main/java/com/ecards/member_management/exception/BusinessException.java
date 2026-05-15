package com.ecards.member_management.exception;

import com.ecards.member_management.common.ErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常类
 * 用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造方法 - 使用错误码和错误信息
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法 - 使用错误码枚举
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构造方法 - 使用错误码枚举和自定义错误信息
     *
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }

    /**
     * 构造方法 - 使用错误信息（默认错误码99999）
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 99999;
        this.message = message;
    }
}

