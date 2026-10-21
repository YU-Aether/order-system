package com.team.orderserver.module.order.service;

import com.team.orderserver.module.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 订单详情缓存（Cache-Aside）。命中后仍需在业务层做归属校验。
 *
 * <p><b>版本号约定</b>：Java record 的 serialVersionUID 固定为 0，给 {@link OrderResponse}
 * 增删字段后，Redis 中的旧缓存仍能被成功反序列化，缺失字段取默认值 null，且不会触发降级。
 * 因此<b>每次调整缓存 DTO 结构，必须递增 {@link #CACHE_NAME} 的版本号</b>，让旧缓存自动失效。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheService {

    private static final String CACHE_NAME = "order:detail:v2";

    private final CacheManager cacheManager;

    public OrderResponse get(String orderNo) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return null;
        }
        try {
            return cache.get(orderNo, OrderResponse.class);
        } catch (Exception ex) {
            log.warn("读取订单缓存失败，降级查库 | orderNo={} | {}", orderNo, ex.getMessage());
            return null;
        }
    }

    public void put(OrderResponse response) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return;
        }
        try {
            cache.put(response.orderNo(), response);
        } catch (Exception ex) {
            // 缓存属于旁路能力，写入失败不影响主流程
            log.warn("写入订单缓存失败 | orderNo={} | {}", response.orderNo(), ex.getMessage());
        }
    }

    public void evict(String orderNo) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return;
        }
        try {
            cache.evict(orderNo);
        } catch (Exception ex) {
            log.warn("清除订单缓存失败 | orderNo={} | {}", orderNo, ex.getMessage());
        }
    }
}
