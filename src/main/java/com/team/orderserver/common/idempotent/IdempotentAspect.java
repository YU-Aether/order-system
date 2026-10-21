package com.team.orderserver.common.idempotent;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.constant.RedisKeys;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 幂等切面：基于 Redis SETNX 实现，业务失败时主动释放 Key 以便客户端重试。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        String idempotencyKey = request == null ? null : request.getHeader(IDEMPOTENCY_HEADER);
        if (!StringUtils.hasText(idempotencyKey)) {
            // 未传幂等键时由业务层的分布式锁兜底
            return joinPoint.proceed();
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String redisKey = RedisKeys.IDEMPOTENT + idempotent.keyPrefix() + ":"
                + (userId == null ? "anonymous" : userId) + ":" + idempotencyKey;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofSeconds(idempotent.timeoutSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("重复请求被拦截, key={}", redisKey);
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
