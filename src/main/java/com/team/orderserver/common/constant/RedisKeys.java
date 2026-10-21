package com.team.orderserver.common.constant;

/**
 * Redis Key 统一收口，避免散落在业务代码中导致前缀冲突。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 下单分布式锁：order:lock:create:{userId} */
    public static final String ORDER_CREATE_LOCK = "order:lock:create:";

    /** 关单分布式锁：order:lock:close:{orderNo} */
    public static final String ORDER_CLOSE_LOCK = "order:lock:close:";

    /** 幂等控制：idempotent:{prefix}:{userId}:{key} */
    public static final String IDEMPOTENT = "idempotent:";

    /** 订单超时延迟队列（ZSet，score 为到期时间戳） */
    public static final String ORDER_TIMEOUT_ZSET = "order:timeout:zset";

    /** 登出令牌黑名单：auth:blacklist:{jti} */
    public static final String TOKEN_BLACKLIST = "auth:blacklist:";
}
