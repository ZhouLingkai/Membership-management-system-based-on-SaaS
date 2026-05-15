package com.ecards.member_management.exception;

import com.ecards.member_management.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各种异常，返回标准的Result格式响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     *
     * @param e BusinessException
     * @return Result对象
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理空指针异常
     *
     * @param e NullPointerException
     * @return Result对象
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常: ", e);
        return Result.fail(99999, "系统异常：空指针错误，请联系技术支持处理");
    }

    /**
     * 处理SQL异常
     *
     * @param e SQLException
     * @return Result对象
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSQLException(SQLException e) {
        log.error("数据库异常: SQLState={}, ErrorCode={}, Message={}", 
                  e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
        return Result.fail(99999, "系统异常：数据库操作失败，请联系技术支持处理");
    }

    /**
     * 处理参数校验异常（@Valid）
     *
     * @param e MethodArgumentNotValidException
     * @return Result对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验异常: {}", errorMessage, e);
        return Result.fail(99999, "参数校验失败: " + errorMessage);
    }

    /**
     * 处理参数绑定异常
     *
     * @param e BindException
     * @return Result对象
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数绑定异常: {}", errorMessage, e);
        return Result.fail(99999, "参数绑定失败: " + errorMessage);
    }

    /**
     * 处理非法参数异常
     *
     * @param e IllegalArgumentException
     * @return Result对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage(), e);
        return Result.fail(99999, "参数错误: " + e.getMessage());
    }

    /**
     * 处理其他未捕获的异常
     *
     * @param e Exception
     * @return Result对象
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(99999, "系统异常，请联系技术支持处理");
    }
}

