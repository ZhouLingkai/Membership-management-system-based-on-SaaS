package com.ecards.member_management.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应类
 * 用于封装所有API接口的返回结果
 *
 * @param <T> 响应数据的泛型类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码：200 为成功，非 200 为失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳（毫秒级）
     */
    private Long timestamp;

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, System.currentTimeMillis());
    }

    /**
     * 成功响应（带自定义消息）
     *
     * @param message 响应消息
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null, System.currentTimeMillis());
    }

    /**
     * 成功响应（带数据和自定义消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }

    /**
     * 失败响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 失败响应（默认错误码99999）
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(99999, message, null, System.currentTimeMillis());
    }
}

