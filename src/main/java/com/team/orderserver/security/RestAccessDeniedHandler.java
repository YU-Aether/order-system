package com.team.orderserver.security;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.web.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 已登录但权限不足时返回统一 JSON。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":%d,"message":"%s","data":null,"traceId":"%s"}
                """.formatted(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(),
                TraceContext.getTraceId() == null ? "" : TraceContext.getTraceId()));
    }
}
