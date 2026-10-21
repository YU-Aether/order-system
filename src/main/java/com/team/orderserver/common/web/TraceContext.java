package com.team.orderserver.common.web;

import org.slf4j.MDC;

/**
 * 链路追踪上下文，基于 SLF4J MDC 实现，贯穿 HTTP 请求与异步线程。
 */
public final class TraceContext {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(MDC_KEY);
    }

    public static void set(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
