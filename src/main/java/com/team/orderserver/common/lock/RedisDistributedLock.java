package com.team.orderserver.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis 的分布式锁。
 * <p>加锁使用 SET NX PX 保证原子性，解锁使用 Lua 脚本校验持有者，避免误删他人锁。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 立即尝试加锁，不等待。成功返回持有凭证（解锁用），失败返回 null。
     */
    public String tryLock(String key, Duration ttl) {
        return tryLock(key, Duration.ZERO, ttl);
    }

    /**
     * 在 waitTime 内自旋等待加锁。
     */
    public String tryLock(String key, Duration waitTime, Duration ttl) {
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + Math.max(0, waitTime.toMillis());
        do {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            if (Boolean.TRUE.equals(acquired)) {
                return token;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    /**
     * 解锁。只有 token 与锁内值一致时才删除。
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        } catch (Exception ex) {
            log.error("释放分布式锁失败, key={}", key, ex);
        }
    }
}
