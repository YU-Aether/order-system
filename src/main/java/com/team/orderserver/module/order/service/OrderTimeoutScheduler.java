package com.team.orderserver.module.order.service;

import com.team.orderserver.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis ZSet 的延迟任务调度器。
 * <p>score 为订单到期时间戳，定时任务按 score 区间拉取到期订单。</p>
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final StringRedisTemplate redisTemplate;

    public void schedule(String orderNo, LocalDateTime expireAt) {
        long score = expireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redisTemplate.opsForZSet().add(RedisKeys.ORDER_TIMEOUT_ZSET, orderNo, score);
    }

    public void remove(String orderNo) {
        redisTemplate.opsForZSet().remove(RedisKeys.ORDER_TIMEOUT_ZSET, orderNo);
    }

    /**
     * 拉取已到期的订单号（不删除，由关单成功后显式移除；失败可自动重试）。
     */
    public List<String> pollExpired(int limit) {
        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(RedisKeys.ORDER_TIMEOUT_ZSET, 0, System.currentTimeMillis(), 0, limit);
        return members == null ? List.of() : new ArrayList<>(members);
    }
}
