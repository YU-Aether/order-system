package com.team.orderserver.common.api;

import com.team.orderserver.common.web.TraceContext;

import java.io.Serializable;

/**
 * 统一响应体。所有接口（含异常）都返回该结构，便于前端与网关统一处理。
 */
public record Result<T>(int code, String message, T data, String traceId, long timestamp) implements Serializable {

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data,
                TraceContext.getTraceId(), System.currentTimeMillis());
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return error(resultCode, resultCode.getMessage());
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null, TraceContext.getTraceId(), System.currentTimeMillis());
    }
}
