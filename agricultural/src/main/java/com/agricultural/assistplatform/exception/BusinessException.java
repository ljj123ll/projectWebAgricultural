package com.agricultural.assistplatform.exception;

import com.agricultural.assistplatform.common.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.SERVER_ERROR;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
