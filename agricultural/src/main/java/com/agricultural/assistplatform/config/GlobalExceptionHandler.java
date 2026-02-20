package com.agricultural.assistplatform.config;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，保证统一响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce("", (a, b) -> a + b);
        return Result.fail(ResultCode.BAD_REQUEST, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce("", (a, b) -> a + b);
        return Result.fail(ResultCode.BAD_REQUEST, msg.isEmpty() ? "参数错误" : msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.SERVER_ERROR, "系统繁忙，请稍后重试");
    }
}
